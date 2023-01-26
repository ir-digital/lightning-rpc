package de.seepex.service;

import de.seepex.domain.Param;
import de.seepex.domain.RegisteredCache;
import de.seepex.domain.RpcCacheResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CacheService {

    private SpxCacheManager spxCacheManager;
    private CacheContainer cacheContainer;

    final static boolean EXECUTED = true;
    final static boolean FOUND = true;

    @Autowired
    public CacheService(SpxCacheManager spxCacheManager, CacheContainer cacheContainer) {
        this.spxCacheManager = spxCacheManager;
        this.cacheContainer = cacheContainer;
    }

    /**
     * This will extract the cache key from the annotation.
     * Usage:
     *     Given params [new Param('id', 'foo'), new Param('name', 'bar')]
     *       #id         --> returns 'foo'
     *       #id | #name --> returns 'foo | bar'
     *
     * @param params
     * @return
     */
    private String getCacheKey(Param... params) {
        StringBuilder result = new StringBuilder();

        for(int i = 0; i < params.length; i++) {
            Param param = params[i];

            if(i > 0 && i+1 <= params.length) {
                result.append("|");
            }
            
            result.append(param.getValue().toString());

        }

        return result.toString();
    }

    public RpcCacheResult getCacheResult(String serviceId, String methodName, Param... params) {
        RegisteredCache registeredCache = cacheContainer.get(serviceId, methodName);
        if(registeredCache == null) {
            return new RpcCacheResult(false, false, null);
        }

        String cacheKey = getCacheKey(params);

        CacheResult cacheResult = spxCacheManager.get(cacheKey, registeredCache);
        if(cacheResult.isFound()) {
            return new RpcCacheResult(EXECUTED, FOUND, cacheResult.getResult());
        }

        // if we are here nothing was found in cache -> maybe expired?
        return new RpcCacheResult(EXECUTED, !FOUND, null);
    }
}
