package com.restApi.testRunners;

import io.cucumber.testng.AbstractTestNGCucumberTests;
import io.cucumber.testng.CucumberOptions;

@CucumberOptions(
		features = {"src\\test\\java\\com\\restApi\\features"},
				glue = {"com.restApi.stepDefinitions"}, //specify hooks location if not present in the same package as step defs
		        plugin = {"com.aventstack.extentreports.cucumber.adapter.ExtentCucumberAdapter:"},
		        monochrome = true,
				publish = true
		)
public class RunCucumberTest extends AbstractTestNGCucumberTests {
}