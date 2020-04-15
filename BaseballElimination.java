/* *****************************************************************************
 *  Name:
 *  Date:
 *  Description:
 **************************************************************************** */

import edu.princeton.cs.algs4.FlowEdge;
import edu.princeton.cs.algs4.FlowNetwork;
import edu.princeton.cs.algs4.FordFulkerson;
import edu.princeton.cs.algs4.In;
import edu.princeton.cs.algs4.StdOut;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BaseballElimination {
    private final Map<String, Integer> teamsToId = new HashMap<>();
    private final Map<Integer, String> idToTeams = new HashMap<>();

    private final int teamCount;

    private final int[] wins;
    private final int[] losses;
    private final int[] remaining;
    private final int[][] remainingGamesOpposingTeams;

    // create a baseball division from given filename in format specified below
    public BaseballElimination(String filename) {
        In in = new In(filename);
        teamCount = in.readInt();

        remainingGamesOpposingTeams = new int[teamCount][teamCount];
        wins = new int[teamCount];
        losses = new int[teamCount];
        remaining = new int[teamCount];

        for (int i = 0; i < teamCount; i++) {
            String teamName = in.readString();
            teamsToId.put(teamName, i);
            idToTeams.put(i, teamName);

            wins[i] = in.readInt();
            losses[i] = in.readInt();
            remaining[i] = in.readInt();

            for (int j = 0; j < teamCount; j++) {
                remainingGamesOpposingTeams[i][j] = in.readInt();
            }
        }
    }

    // number of teams
    public int numberOfTeams() {
        return teamCount;
    }

    // all teams
    public Iterable<String> teams() {
        return teamsToId.keySet();
    }

    // number of wins for given team
    public int wins(String team) {
        return wins[getTeamOrThrowInvalid(team)];
    }

    // number of losses for given team
    public int losses(String team) {
        return losses[getTeamOrThrowInvalid(team)];
    }

    // number of remaining games for given team
    public int remaining(String team) {
        return remaining[getTeamOrThrowInvalid(team)];
    }

    // number of remaining games between team1 and team2
    public int against(String team1, String team2) {
        return remainingGamesOpposingTeams[getTeamOrThrowInvalid(team1)]
                [getTeamOrThrowInvalid(team2)];
    }

    // is given team eliminated?
    public boolean isEliminated(String team) {
        return certificateOfElimination(team) != null;
    }

    // subset R of teams that eliminates given team; null if not eliminated
    public Iterable<String> certificateOfElimination(String team) {
        validateTeam(team);

        for (int i = 0; i < teamCount; i++) {
            if (triviallyEliminated(team, i)) {
                return Collections.singleton(idToTeams.get(i));
            }
        }

        int gameCount = (teamCount) * (teamCount - 1) / 2;
        FordFulkerson fordFulkerson = solveMaxFlow(team, gameCount);
        return certificateOfElimination(gameCount, fordFulkerson);
    }

    public static void main(String[] args) {
        BaseballElimination division = new BaseballElimination(args[0]);
        for (String team : division.teams()) {
            if (division.isEliminated(team)) {
                StdOut.print(team + " is eliminated by the subset R = { ");
                for (String t : division.certificateOfElimination(team)) {
                    StdOut.print(t + " ");
                }
                StdOut.println("}");
            }
            else {
                StdOut.println(team + " is not eliminated");
            }
        }
    }

    private void validateTeam(String team) {
        if (!teamsToId.containsKey(team)) {
            throw new IllegalArgumentException();
        }
    }

    private int getTeamOrThrowInvalid(String team) {
        validateTeam(team);
        return teamsToId.get(team);
    }

    private boolean triviallyEliminated(String team, int i) {
        return !team.equals(idToTeams.get(i)) && wins(team) + remaining(team) < wins[i];
    }

    private FordFulkerson solveMaxFlow(String team, int gameCount) {
        int verticesCount = teamCount + gameCount + 2; // +2, source and sink
        int source = verticesCount - 2;
        int sink = verticesCount - 1;
        FlowNetwork flowNetwork = createFlowNetwork(team, gameCount, source, sink);
        return new FordFulkerson(flowNetwork, source, sink);
    }

    private FlowNetwork createFlowNetwork(String team, int gameCount, int source, int sink) {
        FlowNetwork flowNetwork = new FlowNetwork(teamCount + gameCount + 2);

        int vertex = 0;
        for (int col = 0; col < teamCount; col++) {
            for (int row = col + 1; row < teamCount; row++, vertex++) {
                flowNetwork.addEdge(
                        new FlowEdge(source, vertex, remainingGamesOpposingTeams[col][row]));
                flowNetwork
                        .addEdge(new FlowEdge(vertex, col + gameCount, Double.POSITIVE_INFINITY));
                flowNetwork
                        .addEdge(new FlowEdge(vertex, row + gameCount, Double.POSITIVE_INFINITY));
            }
            flowNetwork.addEdge(
                    new FlowEdge(col + gameCount, sink, wins(team) + remaining(team) - wins[col]));
        }
        return flowNetwork;
    }

    private List<String> certificateOfElimination(int gameCount, FordFulkerson fordFulkerson) {
        List<String> teams = new ArrayList<>();
        for (int i = 0; i < teamCount; i++) {
            if (fordFulkerson.inCut(i + gameCount)) {
                teams.add(idToTeams.get(i));
            }
        }
        return teams.isEmpty() ? null : teams;
    }

}