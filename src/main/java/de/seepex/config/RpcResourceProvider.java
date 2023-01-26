package de.seepex.config;

import de.seepex.domain.SpxResource;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
public class RpcResourceProvider implements RpcResourceSupplier {

    public static final String SPX_RPC_JSONDOC_PATH = "/spx-rpc/jsondoc";

    @Override
    public List<SpxResource> getPaths() {
        return Collections.singletonList(new SpxResource("self", "http://127.0.0.1:666" + SPX_RPC_JSONDOC_PATH));
    }

}
