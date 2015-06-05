/*
 * Copyright 1997-2014 Optimatika (www.optimatika.se)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE. 
 */
package org.ojalgo;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

//import org.ojalgo.array.ArrayTest;
//import org.ojalgo.constant.ConstantTest;
//import org.ojalgo.finance.FinanceTest;
//import org.ojalgo.finance.data.DataTest;
//import org.ojalgo.finance.portfolio.PortfolioTest;
//import org.ojalgo.function.FunctionTest;
//import org.ojalgo.function.multiary.MultiaryTest;
//import org.ojalgo.function.polynomial.PolynomialTest;
//import org.ojalgo.machine.MachineTest;
//import org.ojalgo.matrix.MatrixTest;
//import org.ojalgo.matrix.decomposition.DecompositionTest;
//import org.ojalgo.matrix.decomposition.task.TaskTest;
//import org.ojalgo.matrix.store.StoreTest;
//import org.ojalgo.optimisation.integer.IntegerTest;
import org.ojalgo.optimisation.linear.LinearTest;
//import org.ojalgo.optimisation.mps.MpsTest;
//import org.ojalgo.optimisation.quadratic.QuadraticTest;
//import org.ojalgo.random.RandomTest;
//import org.ojalgo.random.process.ProcessTest;
//import org.ojalgo.scalar.ScalarTest;
//import org.ojalgo.series.SeriesTest;
//import org.ojalgo.type.context.ContextTest;

/**
 * @author apete
 */
public abstract class FunctionalityTest extends TestCase {

    public static void main(final String[] args) {
        junit.textui.TestRunner.run(FunctionalityTest.suite());
    }

    public static Test suite() {
        final TestSuite suite = new TestSuite("ojAlgo Functionality Tests");
        //$JUnit-BEGIN$
//        suite.addTest(ArrayTest.suite());
//        suite.addTest(MachineTest.suite());
//        suite.addTest(ConstantTest.suite());
//        suite.addTest(FinanceTest.suite());
//        suite.addTest(DataTest.suite());
//        suite.addTest(PortfolioTest.suite());
//        suite.addTest(FunctionTest.suite());
//        suite.addTest(MultiaryTest.suite());
//        suite.addTest(PolynomialTest.suite());
//        suite.addTest(MatrixTest.suite());
//        suite.addTest(DecompositionTest.suite());
//        suite.addTest(TaskTest.suite());
//        suite.addTest(StoreTest.suite());
//        suite.addTest(IntegerTest.suite());
        suite.addTest(LinearTest.suite());
//        suite.addTest(MpsTest.suite());
//        suite.addTest(QuadraticTest.suite());
//        suite.addTest(RandomTest.suite());
//        suite.addTest(ProcessTest.suite());
//        suite.addTest(ScalarTest.suite());
//        suite.addTest(SeriesTest.suite());
//        suite.addTest(ContextTest.suite());
        //$JUnit-END$
        return suite;
    }

    protected FunctionalityTest() {
        super();
    }

    protected FunctionalityTest(final String name) {
        super(name);
    }

}
