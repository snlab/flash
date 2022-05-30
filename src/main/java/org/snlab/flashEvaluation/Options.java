package org.snlab.flashEvaluation;

import java.util.ArrayList;
import java.util.List;

import com.beust.jcommander.Parameter;

public class Options {
    @Parameter
    public List<String> parameters = new ArrayList<>();

    @Parameter(names = { "-h", "-help" }, help = true)
    public boolean help;

    @Parameter(names = { "-log", "-verbose" }, description = "Level of verbosity")
    public Integer verbose = 1;

    @Parameter(names = "-dataset", required = true, description = "The dataset to be run")
    public String dataset;

    @Parameter(names = "-subspace", description = "The subspace configuration")
    public String subspace;

    @Parameter(names = "-FIMT", arity = 1, description = "Enable/Disable FIMT")
    public Boolean FIMT = true;

    @Parameter(names = "-PAT", arity = 1, description = "Enable/Disable persistent action tree")
    public Boolean persistentActionTree = true;

    @Parameter(names = "-CE2D", arity = 1, description = "Enable/Disable CE2D")
    public Boolean CE2E = true;

    @Parameter(names = "-req", description = "Verification requirement")
    public String requirement;

    @Parameter(names = "-debug", description = "Enable/Disable debug mode")
    public boolean debug = false;
}
