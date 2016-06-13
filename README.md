#What it is

This project is a sudoku puzzle solver that reads an unsolved puzzle 
from a file specifying which cells are already filled and what those 
cells' values are and outputs the solved puzzle after processing with 
some techniques. If the puzzle is not completely solved by the time 
the solver finishes, the output specifies all the known cell values 
and reports all unsolved cells as having a value of zero.

#How to use it

Run `Solver`, specifying the name of the file whose puzzle is to be 
solved as the first command-line argument. The finished puzzle will be 
printed to the standard output stream.

Alternatively, construct a `Solver` with a reference to a file describing a puzzle 
using one of the supported input formats or with a reference to a 
`Puzzle` that's been constructed externally. Then call `solve()` on 
that `Solver`. Once `solve()` returns, the puzzle is as solved as 
the `Solver` can make it be.

##Supported Input Formats

[Sadman Sudoku](https://github.com/fiveham/Sudoku_Solver/blob/master/src/sudoku/parse/SadmanParser.java) 
format and 
[plaintext](https://github.com/fiveham/Sudoku_Solver/blob/master/src/sudoku/parse/TxtParser.java) 
are supported. 
Sadman Sudoku format files' names must end with `.sdk`. Plaintext 
files's names must end with `.txt` and those files must contain 
the 16, 81, 256, etc. `int` tokens that describe the initial state 
of the unsolved puzzle as the first 16, 81, 256, etc. tokens in the 
file, followed by the end of the file or by any non-`int` token. 
An initially empty cell in plaintext format is indicated by a zero.

#How it works

A sudoku puzzle is represented as a 
[bipartite graph](https://en.wikipedia.org/wiki/Bipartite_graph) of 
[truth-claims](https://github.com/fiveham/Sudoku_Solver/blob/master/src/sudoku/Claim.java) 
about the values of cells and pedantic statements 
of the [rules](https://github.com/fiveham/Sudoku_Solver/blob/master/src/sudoku/Rule.java) 
of a sudoku puzzle. A `Claim` is equivalent to a 
statement such as "Cell r3c5 is 7", regardless whether that statement 
is true or false. A `Rule` is equivalent to a statement such as "The 
value in cell r7c9" (corresponding to the general rule that each 
cell has exactly one value) or "The 1 in row 3" (corresponding to the 
general rule that each row has exactly one occurrence of each value).

Initially a `Puzzle` is fully connected. Every `Claim` is connected to 
four `Rule`s and every `Rule` is connected to 4, 9, 16, etc. `Claim`s, 
corresponding to the side-length of the puzzle. As information is added 
by adding initial values and by performing solution techniques, `Claim`s 
are determined false, and all edges of those false `Claim`s are removed.

At a given point in time, given a `Rule`, exactly one of the elements of 
that `Rule` is true and all the others are false. As such, whenever it 
is determined that a certain `Claim` is not the true `Claim` in a certain 
`Rule`, that `Claim` is simply false and is not the true `Claim` of any 
other `Rule`; so, all its edges with `Rule`s can be removed. Conversely, 
when a `Claim` is known to be true, all of the `Rule`s to which is was 
initially connected are still connected to it but they each have that 
true `Claim` as their only connected neighbor. As such, when a `Puzzle` 
is completely solved, its backing graph is extremely disjoint, every 
false `Claim` completely disconnected from the rest of the graph and 
every true `Claim`'s `Rule`s connected only to that true `Claim`. A solved 
`Puzzle` has 64, 729, 4096, etc. connected components, each of which 
has exactly one `Claim`.

#Theory and Background

This style of interpreting a sudoku puzzle as a bipartite graph of 
claims and rules was implemented in order to implement the 
[Sledgehammer](http://onigame.livejournal.com/20626.html) concept 
for solving sudoku puzzles. The concept is discussed again 
[here](http://onigame.livejournal.com/18580.html).

The Sledgehammer technique holds that every cell, row-value pair, 
column-value pair, and box-value pair can be interpreted as a set 
of cell-value statements such that exactly one statement in each set 
is true and all the others in the same set are false. Any cell-value 
statement known to be false can be safely removed from all the sets 
of which it is a member, since the true statement in each such set 
is still present.

Solving with the Sledgehammer technique means we identify some source `Rule`s 
and some recipient `Rule`s that are interconnected so that no matter what 
the true solution-state is among the source `Rule`s, all those cell-value 
statements in the recipients but not in any of the sources must be false.

Implementing the puzzle as a collection of sets of cell-value statements 
(`Claim`s) is simple, but in order to efficiently remove a statement 
that has been determined false from all four sets of which it is a member, 
each statement needs to have references to those four sets; otherwise, 
we have to brute-force our way through all the sets to find this statement.

The transition from being a collection of sets of statements to being a 
bipartite graph of rules and statements (`Claim`s) comes about because 
we need to be able to tell how `Rule`s are connected in order to ascertain 
the validity of a possible Sledgehammer scenario designed by brute force. 
With the sets-of-statements model, it's trivial to determine if two `Rule`s 
are connected (they intersect), but any more complex connection relationship 
is difficult to describe or verbose. Of particular importance is 
the fact that the source `Rule`s of a Sledgehammer solution scenario must all 
be disjoint from one another yet they all must be connected together via the 
recipient `Rule`s.

If the source `Rule`s and recipient `Rule`s are not interconnected, appropriately, 
then either the Sledgehammer scenario is invalid or the Sledgehammer scenario 
is the union of multiple valid Sledgehammer scenarios each of which has as 
smaller overall size than that of this Sledgehammer scenario, in which case 
they must already have been found and resolved, having all non-source recipient 
`Claim`s falsified. There is no way that a Sledgehammer scenario with multiple 
connected components can be worthwhile to analyse.

In order to prove that a possible Sledgehammer scenario is valid, `Rule`s 
must be treated as nodes in a graph and the nodes' connections with the other 
nodes in the Sledgehammer scenario must be analysed. Finding Sledgehammer 
scenarios is mostly brute force; so, regenerating (or managing a cache of) 
"node" wrappers around these `Rule` sets is either inefficient or 
clunky, respectively. Enabling the `Rule`s to *be* their own nodes from the 
start dodges both of these issues.

In assessing the validity of a Sledgehammer scenario, only `Rule`s need 
to be nodes in the puzzle graph: Sources connect to recipients and non-participants; 
recipients connect to sources and non-participants. However, when it comes time 
to remove some of those edges because a `Claim` is known false, things are awkward. 
Falsifying a `Claim` means removing it from all the sets that contain it, and that 
means removing the fact that those sets connect to each other (through that `Claim`). 
Given four `Rule`s such that the members of any pair of them share only a single 
`Claim`, falsifying that `Claim` means removing *six* edges linking pairs of 
those `Rule`s, turning a completely connected subgraph into a completely disjoint 
subgraph. Alternatively, given four `Rule`s that all share a certain `Claim` in 
common, falsifying that `Claim` will mean removing some but not necessarily all 
of the six edges connecting those `Rule`s. If there were any other `Claim`s shared 
by the box `Rule` and the row `Rule` or the column `Rule`, then the box `Rule` and 
the other non-cell `Rule`(s) remain connected although the cell `Rule` disconnects 
from the other three and the row or column `Rule` that was connected to the box 
`Rule` by only one `Claim` (if there is such a row or column `Rule`) disconnects 
from the box and column/row `Rule`.

Rather than removing edges according to a complex set of requirements and testing 
to find out whether two ostensibly disconnectable `Rule`s really should be 
disconnected, we can treat those totally connected subgraphs around a `Claim` that 
sometimes need to be partially *or* fully disassembled as a collection of nodes 
that are all connected to another node in the middle. That node is that `Claim`, and 
removing it from the graph (and removing all connections it has) removes the 
removeable connections between any two `Rule`s that need to be disconnected when that 
`Claim` is falsified but leaves intact any connections that need to be preserved 
because those connected `Rule`s are still connected through some other `Claim`(s).
