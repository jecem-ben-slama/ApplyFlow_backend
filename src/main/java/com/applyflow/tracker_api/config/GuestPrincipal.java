package com.applyflow.tracker_api.config;

import java.io.Serializable;

public class GuestPrincipal implements Serializable {

    private static final long serialVersionUID = 1L;

    private final Long id;

    public GuestPrincipal(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }
}