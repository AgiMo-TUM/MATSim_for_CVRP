package org.matsim.project.parser;

import lombok.*;
import org.matsim.project.businessModels.CVRP;

import java.nio.file.Path;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PickleParser {
    private Path PathToPickleFile;

    public CVRP parseCVRPOutput() {

        return null;
    }
}
