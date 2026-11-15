package com.instalert_backend.bdd;
import org.junit.platform.suite.api.*;
import static io.cucumber.junit.platform.engine.Constants.*;
@Suite @IncludeEngines("cucumber") @SelectClasspathResource("features")
@ConfigurationParameter(key=GLUE_PROPERTY_NAME,value="com.instalert_backend.bdd")
@ConfigurationParameter(key=PLUGIN_PROPERTY_NAME,value="summary,json:target/cucumber/incidents.json")
public class IncidentsBehaviorTest {}
