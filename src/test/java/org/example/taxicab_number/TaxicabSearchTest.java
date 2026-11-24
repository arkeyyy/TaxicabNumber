package org.example.taxicab_number;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

class TaxicabSearchTest {
    @Test
    void sampleRangesAndMatches() {
        var ranges = TaxicabSearch.ranges(16000, 4);
        assertEquals(List.of(1600, 3200, 4800, 6400),
                ranges.stream().map(TaxicabSearch.Range::size).toList());
        var matches = new ArrayList<Integer>();
        for (var range : ranges) {
            matches.addAll(search(range.first(), range.last()));
        }
        assertEquals(List.of(1729, 4104, 13832), matches);
    }

    @Test
    void partitionsCoverEachIntegerOnceAtTheInputLimits() {
        for (int num : List.of(1, 2, 10, 37, 64, 65, 75, 512, 2079, 2080, 2081, Integer.MAX_VALUE)) {
            for (int count = 1; count <= Math.min(num, 64); count++) {
                var ranges = TaxicabSearch.ranges(num, count);
                assertEquals(count, ranges.size());
                long covered = 0;
                for (var range : ranges) {
                    assertEquals(covered + 1, range.first());
                    assertTrue(range.size() > 0);
                    covered += range.size();
                }
                assertEquals(num, covered);
                long distinct = ranges.stream().map(TaxicabSearch.Range::size).distinct().count();
                long minimumForOneMoreWidth = count + distinct * (distinct + 1) / 2;
                assertTrue(distinct == count || minimumForOneMoreWidth > num);
            }
        }
    }

    @Test
    void upperAndLowerEndpointsAreInclusiveAndPairsAreUnordered() {
        assertEquals(List.of(), search(1, 1));
        assertEquals(List.of(), search(1, 1728));
        assertEquals(List.of(1729), search(1729, 1729));
        assertEquals(List.of(), search(1730, 4103));
    }

    @Test
    void aThirdRepresentationDoesNotDuplicateAMatch() {
        assertEquals(List.of(87539319), search(87539319, 87539319));
    }

    @Test
    void fullBlocksAndMaximumEndpointAgreeWithAnIndependentPairCount() {
        int first = Integer.MAX_VALUE - 140000;
        byte[] counts = new byte[Integer.MAX_VALUE - first + 1];
        for (long a = 1; 2 * a * a * a <= Integer.MAX_VALUE; a++) {
            for (long b = a; ; b++) {
                long sum = a * a * a + b * b * b;
                if (sum > Integer.MAX_VALUE) {
                    break;
                }
                if (sum >= first) {
                    counts[(int) (sum - first)]++;
                }
            }
        }
        var expected = new ArrayList<Integer>();
        for (int i = 0; i < counts.length; i++) {
            if (counts[i] >= 2) {
                expected.add(first + i);
            }
        }
        assertEquals(expected, search(first, Integer.MAX_VALUE));
    }

    @Test
    void cancellationDiscardsAnUnfinishedSearch() {
        var checked = new AtomicLong();
        var range = new TaxicabSearch.Range(1, Integer.MAX_VALUE);
        assertThrows(CancellationException.class,
                () -> TaxicabSearch.search(range, () -> checked.get() > 0, checked::set));
        assertTrue(checked.get() > 0 && checked.get() < range.size());
    }

    private static List<Integer> search(int first, int last) {
        var range = new TaxicabSearch.Range(first, last);
        var progress = new AtomicLong();
        var matches = TaxicabSearch.search(range, () -> false, checked -> {
            assertTrue(checked > progress.getAndSet(checked));
            assertTrue(checked <= range.size());
        });
        assertEquals(range.size(), progress.get());
        return matches;
    }
}
