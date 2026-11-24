# Taxicab Number

Taxicab Number is a JavaFX desktop application that finds integers with at least two distinct representations as the sum of two positive cubes. For example, 1729 qualifies because 1³ + 12³ and 9³ + 10³ both equal 1729. Reversing the roots counts as the same pair.

## Search

The upper limit is inclusive and accepts values from 1 through 2,147,483,647. The thread count ranges from 1 through the smaller of the upper limit and 64. The defaults are 10,000 and four threads.

Each worker checks a contiguous range. Its circular indicator shows the fraction of that range checked. Completed searches display the matches in ascending order and report how many numbers qualified, how many had fewer than two representations, and how many were checked. Stopping a search clears its partial results.

## Example

With an upper limit of 16,000 and four threads, the matches are **1729, 4104, and 13832**.

| Taxicab numbers | Other numbers | Total checked |
| ---: | ---: | ---: |
| 3 | 15,997 | 16,000 |

## Build

The project uses Java 17 or later, JavaFX 21, and the included Maven wrapper.

| Maven goal | Windows | macOS or Linux |
| --- | --- | --- |
| Desktop application | `.\mvnw.cmd javafx:run` | `sh mvnw javafx:run` |
| Test suite | `.\mvnw.cmd test` | `sh mvnw test` |
