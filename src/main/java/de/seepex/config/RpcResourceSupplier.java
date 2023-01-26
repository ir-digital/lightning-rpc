package de.seepex.config;

import de.seepex.domain.SpxResource;

import java.util.List;

public interface RpcResourceSupplier {

    List<SpxResource> getPaths();
}
