package org.matsim.project.businessModels;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CVRPNode {
    private long nodeID;
    private int demand;
    private int x;
    private int y;
}
