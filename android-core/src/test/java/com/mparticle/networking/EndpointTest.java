package com.mparticle.networking;

import org.junit.Test;

import static junit.framework.Assert.assertEquals;

public class EndpointTest {

    @Test
    public void parseEnumTest() {
        for (MParticleBaseClientImpl.Endpoint endpoint: MParticleBaseClientImpl.Endpoint.values()) {
            assertEquals(endpoint, MParticleBaseClientImpl.Endpoint.parseInt(endpoint.value));
        }
    }
}
