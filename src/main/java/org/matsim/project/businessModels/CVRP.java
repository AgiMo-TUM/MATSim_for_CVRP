package org.matsim.project.businessModels;


import lombok.*;
import org.postgresql.core.Tuple;

import java.util.ArrayList;
import java.util.Dictionary;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CVRP {
    private ArrayList<CVRPNode> nodes;
    private ArrayList<CVRPArc> arcs;
    private int vehicleCapacity;
    private int nbVehicle;
}
