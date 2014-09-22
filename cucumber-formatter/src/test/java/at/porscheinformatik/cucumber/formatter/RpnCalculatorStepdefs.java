package at.porscheinformatik.cucumber.formatter;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.FileInputStream;
import java.util.List;

import cucumber.api.Scenario;
import cucumber.api.java.After;
import cucumber.api.java.Before;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;

public class RpnCalculatorStepdefs
{
    private RpnCalculator calc;

    @Given("^a calculator I just turned on$")
    public void a_calculator_I_just_turned_on()
    {
        calc = new RpnCalculator();
    }

    @When("^I add (\\d+) and (\\d+)$")
    public void adding(int arg1, int arg2)
    {
        calc.push(arg1);
        calc.push(arg2);
        calc.push("+");
    }

    @Given("^I press (.+)$")
    public void I_press(String what)
    {
        calc.push(what);
    }

    @Then("^the result is (\\d+)$")
    public void the_result_is(double expected)
    {
        assertEquals(expected, calc.value());
    }

    @Before({"~@foo"})
    public void before()
    {
        System.out.println("Runs before scenarios *not* tagged with @foo");
    }

    @After
    public void after(Scenario scenario)
    {
        try
        {
            FileInputStream fis;
            fis = new FileInputStream(new File("C:\\tmp\\belegStorno.png"));
            scenario.embed(org.apache.commons.io.IOUtils.toByteArray(fis), "image/bmp");
        }
        catch (java.lang.Exception e)
        {
            e.printStackTrace();
        }
    }

    @Given("^the previous entries:$")
    public void thePreviousEntries(List<Entry> entries)
    {
        for (Entry entry : entries)
        {
            calc.push(entry.first);
            calc.push(entry.second);
            calc.push(entry.operation);
        }
    }

    public class Entry
    {
        Integer first;
        Integer second;
        String operation;
    }
}
