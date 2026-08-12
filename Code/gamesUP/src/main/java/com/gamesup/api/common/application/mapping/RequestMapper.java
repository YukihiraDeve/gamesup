package com.gamesup.api.common.application.mapping;

@FunctionalInterface
public interface RequestMapper<REQUEST, TARGET> {

	TARGET toTarget(REQUEST request);
}
