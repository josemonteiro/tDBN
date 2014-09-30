tDBN
=====

com.gihub.tDBN.cli.LearnFromFile

```
usage:
 -c,--compact                 Outputs network in compact format, ommiting
                              intra-slice edges. Only works if specified
                              together with -d and with --markovLag 1.
 -d,--dotFormat               Outputs network in dot format, allowing
                              direct use with GraphViz to visualize the
                              graph.
 -i,--inputFile <file>        Input CSV file to be used for network
                              learning.
 -l,--markovLag <arg>         Maximum Markov lag to be considered, which
                              is the longest distance between connected
                              time slices. Default is 1, allowing edges
                              from one preceeding slice.
 -o,--outputFile <file>       Writes output to <file>. If not supplied,
                              output is written to terminal.
 -p,--numParents <arg>        Maximum number of parents from preceding
                              time slice(s).
 -r,--root <node>             Root node of the intra-slice tree. By
                              default, root is arbitrary.
 -s,--scoringFunction <arg>   Scoring function to be used, either MDL or
                              LL. MDL is used by default.
```
