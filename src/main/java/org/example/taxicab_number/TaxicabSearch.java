package org.example.taxicab_number;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.function.BooleanSupplier;
import java.util.function.LongConsumer;
import java.util.stream.LongStream;

final class TaxicabSearch {
    private static final int BLOCK_SIZE = 65_536;
    private static final long[] CUBES = LongStream
            .iterate(1, root -> cube(root) <= Integer.MAX_VALUE, root -> root + 1)
            .map(TaxicabSearch::cube)
            .toArray();

    private TaxicabSearch() {
    }

    record Range(int first, int last) {
        Range {
            if (first < 1 || last < first) {
                throw new IllegalArgumentException("Invalid search range");
            }
        }

        int size() {
            return last - first + 1;
        }
    }

    static List<Range> ranges(int num, int cnt) {
        if (num < 1 || cnt < 1 || cnt > Math.min(num, 64)) {
            throw new IllegalArgumentException("Invalid upper limit or thread count");
        }
        // Every worker needs one number. A staircase of k distinct widths needs
        // an additional 1 + 2 + ... + (k - 1) numbers.
        int distinctWidths = cnt;
        while (cnt + distinctWidths * (distinctWidths - 1) / 2 > num) {
            distinctWidths--;
        }
        int totalWeight = cnt + distinctWidths * (distinctWidths - 1) / 2;
        int[] widths = new int[cnt];
        int assigned = 0;
        for (int i = 0; i < cnt; i++) {
            int weight = Math.max(1, i + 1 - (cnt - distinctWidths));
            widths[i] = (int) ((long) num * weight / totalWeight);
            assigned += widths[i];
        }
        for (int i = cnt - (num - assigned); i < cnt; i++) {
            widths[i]++;
        }

        List<Range> ranges = new ArrayList<>(cnt);
        int end = 0;
        for (int width : widths) {
            ranges.add(new Range(end + 1, end + width));
            end += width;
        }
        return ranges;
    }

    static List<Integer> search(Range range, BooleanSupplier cancelled, LongConsumer progress) {
        int rows = 0;
        while (rows < CUBES.length && 2 * CUBES[rows] <= range.last()) {
            rows++;
        }
        int[] next = new int[rows];
        for (int a = 0; a < rows; a++) {
            int index = Arrays.binarySearch(CUBES, a, CUBES.length, range.first() - CUBES[a]);
            next[a] = index >= 0 ? index : -index - 1;
        }

        List<Integer> matches = new ArrayList<>();
        byte[] counts = new byte[Math.min(BLOCK_SIZE, range.size())];
        for (long first = range.first(); first <= range.last(); first += BLOCK_SIZE) {
            if (cancelled.getAsBoolean()) {
                throw new CancellationException();
            }
            long last = Math.min(first + BLOCK_SIZE - 1, range.last());
            int length = (int) (last - first + 1);
            Arrays.fill(counts, 0, length, (byte) 0);
            for (int a = 0; a < rows && 2 * CUBES[a] <= last; a++) {
                int b = next[a];
                while (b < CUBES.length) {
                    long sum = CUBES[a] + CUBES[b];
                    if (sum > last) {
                        break;
                    }
                    int offset = (int) (sum - first);
                    if (counts[offset] < 2) {
                        counts[offset]++;
                    }
                    b++;
                }
                next[a] = b;
            }
            for (int offset = 0; offset < length; offset++) {
                if (counts[offset] == 2) {
                    matches.add((int) (first + offset));
                }
            }
            progress.accept(last - range.first() + 1);
        }
        return matches;
    }

    private static long cube(long root) {
        return root * root * root;
    }
}
