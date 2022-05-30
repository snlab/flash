package org.snlab.flash.CE2D;

import java.util.HashMap;
import java.util.Map;

import org.snlab.flash.model.ModelManager;

/**
 * This class maps (epoch, subspace) to model managers
 */
public class ModelMapper {
    private Map<String, ModelManager> epochToModelManager;

    public ModelMapper() {
        this.epochToModelManager = new HashMap<>();
    }

    public void createModelManager() {

    }
}
