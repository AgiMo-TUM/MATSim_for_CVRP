package org.matsim.project.parser;

import org.json.JSONArray;
import org.json.JSONObject;
import org.matsim.project.businessModels.CVRP;
import org.matsim.project.businessModels.CVRPArc;
import org.matsim.project.businessModels.CVRPNode;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;

public class JSONParser {
    private Path PathToJSONFile;
    private JSONObject jsonObject;

    public JSONParser(Path PathToJSONFile) throws IOException {
        this.PathToJSONFile = PathToJSONFile;

        BufferedReader reader = new BufferedReader(new FileReader(this.PathToJSONFile.toString()));
        String line;
        StringBuilder fileAsString = new StringBuilder();
        while ((line = reader.readLine()) != null) {
            fileAsString.append(line).append("\n");
        }
        this.jsonObject = new JSONObject(fileAsString.toString());
    }

    public CVRP getCVRPInstance() {
        CVRP resultingCVRP = new CVRP();

        resultingCVRP.setNodes(parseForNodes());
        resultingCVRP.setVehicleCapacity(parseForVehicleCapacity());
        resultingCVRP.setNbVehicle(parseForNbVehicles());
        resultingCVRP.setArcs(parseForArcs());

        return resultingCVRP;
    }

    private int parseForNbVehicles() {
        return this.jsonObject.getJSONObject("instance").getInt("nb_vehicles");
    }

    private int parseForVehicleCapacity() {
        return this.jsonObject.getJSONObject("instance").getInt("vehicle_capacity");
    }

    private ArrayList<CVRPNode> parseForNodes() {
        ArrayList<CVRPNode> resultingNodeList = new ArrayList<>();

        JSONArray nodeList = this.jsonObject.getJSONObject("instance").getJSONArray("nodes");

        for (int i = 0; i < nodeList.length(); i++) {
            JSONObject node = nodeList.getJSONObject(i);
            CVRPNode newNode = CVRPNode.builder()
                    .nodeID(node.getInt("node_id"))
                    .demand(node.getInt("demand"))
                    .x(node.getDouble("x"))
                    .y(node.getDouble("y"))
                    .build();
            resultingNodeList.add(newNode);
        }

        return resultingNodeList;
    }

    private ArrayList<CVRPArc> parseForArcs() {
        ArrayList<CVRPArc> arcList = new ArrayList<>();

        JSONArray arcCosts = this.jsonObject.getJSONObject("instance").getJSONArray("arc_costs");
        JSONArray solution = this.jsonObject.getJSONArray("solution");

        for (int i = 0; i < solution.length(); i++) {
            int arcCost = arcCosts.getInt(i);
            JSONArray solutionElem = solution.getJSONArray(i);

            CVRPArc newArc = new CVRPArc();
            newArc.setSource(solutionElem.getInt(0));
            newArc.setDestination(solutionElem.getInt(1));
            newArc.setActiveInSolution(solutionElem.getInt(2) == 1);
            newArc.setArcCost(arcCost);

            arcList.add(i, newArc);
        }

        return arcList;
    }

    @Deprecated
    private ArrayList<ArrayList<Integer>> parseForArcSolutionIndices() {
        ArrayList<ArrayList<Integer>> arcList = new ArrayList<>();

        JSONArray arcSolutionIndices = this.jsonObject.getJSONArray("solution");
        for (int i = 0; i < arcSolutionIndices.length(); i++) {
            JSONArray innerJson = arcSolutionIndices.getJSONArray(i);
            ArrayList<Integer> row = new ArrayList<>();
            for (int j = 0; j < 2; j++) {
                row.add(innerJson.getInt(j));
            }
            arcList.add(row);
        }

        return arcList;
    }

    @Deprecated
    private ArrayList<ArrayList<Integer>> parseForArcIndices() {
        ArrayList<ArrayList<Integer>> arcList = new ArrayList<>();

        JSONArray arcIndices = this.jsonObject.getJSONObject("instance").getJSONArray("arc_index");
        JSONArray source = arcIndices.getJSONArray(0);
        JSONArray dest = arcIndices.getJSONArray(1);
        for (int i = 0; i < arcIndices.getJSONArray(0).length(); i++) {
            ArrayList<Integer> row = new ArrayList<>();
            row.add(source.getInt(i));
            row.add(dest.getInt(i));
            arcList.add(row);
        }

        return arcList;
    }
}
