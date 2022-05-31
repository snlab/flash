package org.snlab.flash.CE2D;

import java.util.HashMap;
import java.util.Map;

import org.snlab.flash.ModelManager.InverseModel;


/**
 * This class maps (epoch, subspace) to model
 */
public class ModelMapper {
    private Map<String, InverseModel> epochToModel;

    public ModelMapper() {
        this.epochToModel = new HashMap<>();
    }

    public void createModel() {

    }
}
