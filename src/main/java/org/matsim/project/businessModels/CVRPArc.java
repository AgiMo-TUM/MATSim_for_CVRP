package org.matsim.project.businessModels;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CVRPArc {
    private int source;
    private int destination;
    private int arcCost;
    private boolean activeInSolution;
}
