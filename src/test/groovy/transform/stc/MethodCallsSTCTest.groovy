/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package groovy.transform.stc

import org.codehaus.groovy.tools.javac.JavaAwareCompilationUnit

import static org.codehaus.groovy.control.customizers.builder.CompilerCustomizationBuilder.withConfig

/**
 * Unit tests for static type checking : method calls.
 */
class MethodCallsSTCTest extends StaticTypeCheckingTestCase {

    private static final boolean IS_PRE_8
    static {
        try {
            Class.forName("java.util.function.Predicate")
            IS_PRE_8 = false
        } catch (ClassNotFoundException e) {
            IS_PRE_8 = true
        }
    }

    @Override
    protected void configure() {
        withConfig(config) {
            imports {
                alias 'A', 'groovy.transform.stc.MethodCallsSTCTest.MyMethodCallTestClass'
                alias 'B', 'groovy.transform.stc.MethodCallsSTCTest.MyMethodCallTestClass2'
                alias 'C', 'groovy.transform.stc.MethodCallsSTCTest.MyMethodCallTestClass3'
            }
        }
    }

    void testMethodCallOnInstance() {
        assertScript '''
            A a = new A()
            assert a.add(1,1)==2
        '''
    }

    void testBestChoiceMethodCallOnInstance() {
        assertScript '''
            A a = new A()
            assert a.add(1d,1d)==3
        '''
    }

    void testMissingInstanceMethod() {
        shouldFailWithMessages '''
            A a = new A()
            assert a.foo(1,1)==2
        ''',
        'Cannot find matching method'
    }

    void testMethodCallOnInstanceWithVarArgs() {
        assertScript '''
            A a = new A()
            assert a.sum(1,1,2)==4
        '''
    }

    void testMethodCallOnInstanceWithVarArgs2() {
        assertScript '''
            A a = new A()
            int[] arr = [1,1,2]
            assert a.sum(arr)==4
        '''
    }

    void testStaticMethodCall() {
        assertScript '''
            String echo = A.echo 'echo'
            assert  echo == 'echo'
        '''
    }

    void testMissingStaticMethod() {
        shouldFailWithMessages '''
            A.missing 'echo'
        ''',
        'Cannot find matching method'
    }

    void testStaticMethodWithVarArgs() {
        assertScript '''
            int mul = A.mul([1,2,3] as int[])
            assert mul == 6
        '''
    }

    void testStaticMethodWithVarArgs2() {
        assertScript '''
            int mul = A.mul(1,2,3)
            assert mul == 6
        '''
    }

    void testStaticMethodCallWithInheritance() {
        assertScript '''
            String echo = B.echo 'echo'
            assert  echo == 'echo'
        '''
    }

    void testStaticMethodCallThroughInstance() {
        assertScript '''
            A a = new A()
            String echo = a.echo 'echo'
            assert echo == 'echo'
        '''
    }

    void testStaticMethodCallOnJDK() {
        assertScript '''
            int[] arr = [3,2,1]
            Arrays.sort(arr)
            assert arr == [1,2,3] as int[]
        '''
    }

    void testStaticMethodCallOnJDK2() {
        assertScript '''
            String[] arr = ['3','2','1']
            Arrays.sort(arr)
            assert arr == ['1','2','3']  as String[]
        '''
    }

    void testStaticMethodCallOnJDK3() {
        assertScript '''
            List arr = ['3','2','1']
            Collections.sort(arr)
            assert arr == ['1','2','3']
        '''
    }

    void testStaticMethodCallOnJDK4() {
        assertScript '''
            List<String> arr = ['3','2','1']
            Collections.sort(arr)
            assert arr == ['1','2','3']
        '''
    }

    void testGenericMethodCall() {
        assertScript '''
            C c = new C()
            String[] args = ['a','b','c']
            assert c.identity(args) == args
        '''
    }

    void testGenericMethodCallWithVarArg() {
        assertScript '''
            C c = new C()
            assert c.identity('a','b','c') == ['a','b','c']
        '''
    }

    void testGenericMethodCallWithVarArgAndSingleArg() {
        assertScript '''
            C c = new C()
            assert c.identity('a') == ['a']
        '''
    }

    void testGenericMethodCallWithVarArgAndNoArg() {
        assertScript '''
            C c = new C()
            assert c.identity() == []
        '''
    }

    void testGenericMethodCall2() {
        assertScript '''
            B c = new B<String>()
            String[] args = ['a','b','c']
            assert c.identity(args) == args
        '''
    }

    void testGenericMethodCall3() {
        shouldFailWithMessages '''
            B c = new B<Integer>()
            String[] args = ['a','b','c']
            assert c.identity(args) == args
        ''',
        'Cannot call groovy.transform.stc.MethodCallsSTCTest$MyMethodCallTestClass2 <Integer>#identity(java.lang.Integer[]) with arguments [java.lang.String[]]'
    }

    // GROOVY-10922
    void testCallToSuperGenerated() {
        assertScript '''
            interface Foo {
                String getString()
                void setString(String s)
            }
            class Bar implements Foo {
                String string
            }
            class Baz extends Bar {
                String object
                @Override
                void setString(String string) {
                    super.setString(string)
                    object = string
                }
            }

            def obj = new Baz()
            obj.setString('xx')
            assert obj.object == 'xx'
            assert obj.string == 'xx'
        '''
    }

    void testMethodCallFromSuperOwner() {
        assertScript '''
            class Child extends groovy.transform.stc.MethodCallsSTCTest.GroovyPage {
                void foo() {
                    createTagBody(1) { ->
                        printHtmlPart(2)
                    }
                }
            }
            new Child()
        '''
    }

    // GROOVY-5175
    void testCallMethodAcceptingArrayWithNull() {
        assertClass '''
            class Main {
                def bar(String[] s) {
                }
                def foo() {
                    bar(null)
                }
            }
        '''
    }

    // GROOVY-5175
    void testCallMethodWithNull() {
        assertClass '''
            class Main {
                def bar(Date date) {
                }
                def foo() {
                    bar(null)
                }
            }
        '''
    }

    // GROOVY-5175
    void testCallMethodWithNullAndAnotherParameter() {
        assertClass '''
            class Main {
                def bar(Date date1, Date date2) {
                }
                def foo() {
                    bar(null, new Date())
                }
            }
        '''
    }

    // GROOVY-5175
    void testAmbiguousCallMethodWithNullAndAnotherParameter() {
        shouldFailWithMessages '''
            class Main {
                def bar(Date date1, Date date2) {
                }
                def bar(String str, Date date) {
                }
                def foo() {
                    bar(null, new Date())
                }
            }
        ''',
        'Reference to method is ambiguous'
    }

    // GROOVY-5175
    void testDisambiguateCallMethodWithNullAndAnotherParameter() {
        assertClass '''
            class Main {
                def bar(Date date1, Date date2) {
                }
                def bar(String str, Date date) {
                }
                def foo() {
                    bar((Date)null, new Date())
                }
            }
        '''
    }

    void testMethodCallArgumentUsingInstanceOf() {
        assertScript '''
            void foo(String str) { 'String' }
            def o
            if (o instanceof String) {
                foo(o)
            }
        '''
    }

    void testShouldFindStaticMethod() {
        assertScript '''
            static String foo(String s) {
                'String'
            }
            foo('String')
        '''
    }

    void testShouldFailWithNoMatchingMethod() {
        shouldFailWithMessages '''
            static String foo(String s) {
                'String'
            }
            static String foo(Integer s) {
                'Integer'
            }
            static String foo(Boolean s) {
                'Boolean'
            }
            ['foo',123,true].each { foo(it) }
        ''',
        'Cannot find matching method'
    }

    void testShouldNotFailThanksToInstanceOfChecks() {
        assertScript '''
            static String foo(String s) {
                'String'
            }
            static String foo(Integer s) {
                'Integer'
            }
            static String foo(Boolean s) {
                'Boolean'
            }
            ['foo',123,true].each {
                if (it instanceof String) {
                    foo((String)it)
                } else if (it instanceof Boolean) {
                    foo((Boolean)it)
                } else if (it instanceof Integer) {
                    foo((Integer)it)
                }
            }
        '''
    }

    void testShouldNotFailThanksToInstanceOfChecksAndWithoutExplicitCasts() {
        assertScript '''
            static String foo(String s) {
                'String'
            }
            static String foo(Integer s) {
                'Integer'
            }
            static String foo(Boolean s) {
                'Boolean'
            }
            ['foo',123,true].each {
                if (it instanceof String) {
                    foo(it)
                } else if (it instanceof Boolean) {
                    foo(it)
                } else if (it instanceof Integer) {
                    foo(it)
                }
            }
        '''
    }

    void testShouldNotFailThanksToInstanceOfChecksAndWithoutExplicitCasts2() {
        assertScript '''
            static String foo(String s) {
                'String'
            }
            static String foo(Integer s) {
                'Integer'
            }
            static String foo(Boolean s) {
                'Boolean'
            }
            ['foo',123,true].each { argument ->
                if (argument instanceof String) {
                    foo(argument)
                } else if (argument instanceof Boolean) {
                    foo(argument)
                } else if (argument instanceof Integer) {
                    foo(argument)
                }
            }
        '''
    }

    void testShouldFailWithMultiplePossibleMethods() {
        shouldFailWithMessages '''
            static String foo(String s) {
                'String'
            }
            static String foo(Integer s) {
                'Integer'
            }
            static String foo(Boolean s) {
                'Boolean'
            }
            ['foo',123,true].each {
                if (it instanceof String || it instanceof Boolean || it instanceof Integer) {
                    foo(it)
                }
            }
        ''',
        'Reference to method is ambiguous'
    }

    void testShouldFailWithMultiplePossibleMethods2() {
        shouldFailWithMessages '''
            static String foo(String s) {
                'String'
            }
            static String foo(Integer s) {
                'Integer'
            }
            static String foo(Boolean s) {
                'Boolean'
            }
            ['foo',123,true].each { argument ->
                if (argument instanceof String || argument instanceof Boolean || argument instanceof Integer) {
                    foo(argument)
                }
            }
        ''',
        'Reference to method is ambiguous'
    }

    void testInstanceOfOnExplicitParameter() {
        assertScript '''
            1.with { obj ->
                if (obj instanceof String) {
                    obj.toUpperCase()
                }
            }
        '''
    }

    void testSAMWithExplicitParameter1() {
        assertScript '''
            public interface SAM {
                boolean run(String var1, Thread th);
            }

            static boolean foo(SAM sam) {
               sam.run("foo",  new Thread())
            }

            static def callSAM() {
                foo { str, th ->
                    str.toUpperCase().equals(th.getName())
                }
            }
        '''
    }

    // GROOVY-8241
    void testSAMWithExplicitParameter2() {
        if (IS_PRE_8) return
        assertScript '''
            static boolean foo(java.util.function.Predicate<? super String> p) {
                p.test('bar')
            }
            foo { it -> it.toUpperCase(); return true }
        '''
    }

    // GROOVY-7061
    void testSAMWithExplicitParameter3() {
        assertScript '''
            List<Integer> nums = [1, 2, 3, -2, -5, 6]
            Collections.sort(nums, { a, b -> a.abs() <=> b.abs() })
        '''
    }

    // GROOVY-7061
    void testSAMWithExplicitParameter4() {
        if (IS_PRE_8) return
        assertScript '''
            def foo(List<String> strings) {
                strings.stream().filter { s -> s.length() < 10 }.toArray()
            }
            def words = ["orange", "sit", "test", "flabbergasted", "honorific"]
            foo(words)
        '''
    }

    void testShouldFailBecauseVariableIsReassigned() {
        shouldFailWithMessages '''
            static String foo(String s) {
                'String'
            }
            def it
            if (it instanceof String) {
                it = new Date()
                foo(it)
            }
        ''',
        'foo(java.util.Date)'
    }

    void testShouldNotFailEvenIfVariableIsReassigned() {
        assertScript '''
            static String foo(int val) {
                'int'
            }
            def it
            if (it instanceof String) {
                it = 123
                foo(it)
            }
        '''
    }

    void testShouldNotFailEvenIfVariableIsReassignedAndInstanceOfIsEmbed() {
        assertScript '''
            static String foo(int val) {
                'int'
            }
            static String foo(Date val) {
                'Date'
            }
            def it
            if (it instanceof String) {
                it = 123
                foo(it)
                if (it instanceof Date) {
                    foo(it)
                }
            }
        '''
    }

    void testOneDefaultParam() {
        assertScript '''
            String m(String val = 'hello') {
                return val.toUpperCase()
            }
            assert m() == 'HELLO'
            assert m('bye') == 'BYE'
        '''
    }

    void testOneDefaultParamWithWrongArgType() {
        shouldFailWithMessages '''
            String m(String val = 'hello') {
                return val.toUpperCase()
            }
            assert m(123) == 'HELLO'
        ''',
        '#m(int)'
    }

    void testOneDefaultParamAndOneWithout() {
        assertScript '''
            String m(String val = 'hello', int append) {
                return val.toUpperCase() + append
            }
            assert m(1) == 'HELLO1'
            assert m('bye',2) == 'BYE2'
        '''
    }

    void testOneDefaultParamAndOneWithoutWithWrongArgType() {
        shouldFailWithMessages '''
            String m(String val = 'hello', int append) {
                return val.toUpperCase() + append
            }
            m('test', new Object())
        ''',
        'm(java.lang.String, java.lang.Object)'
    }

    void testMultipleDefaultArgs() {
        assertScript '''
            String m(String first = 'first', String second, String third = 'third') {
                return first.toUpperCase() + ' ' + second.toUpperCase() + ' ' + third.toUpperCase()
            }
            assert m('hello') == 'FIRST HELLO THIRD'
        '''
    }

    void testMultipleDefaultArgsWithMixedTypes() {
        assertScript '''
            String m(String first = 'first', int second, String third = 'third') {
                return first.toUpperCase() + ' ' + second + ' ' + third.toUpperCase()
            }
            assert m(123) == 'FIRST 123 THIRD'
            assert m('f',123) == 'F 123 THIRD'
            assert m('f',123,'s') == 'F 123 S'
        '''
    }

    void testMultipleDefaultArgsWithMixedTypesAndTooManyArgs() {
        shouldFailWithMessages '''
            String m(String first = 'first', int second, String third = 'third') {
                return first.toUpperCase() + ' ' + second + ' ' + third.toUpperCase()
            }
            m('f',123,'s', 'too many args')
        ''',
        '#m(java.lang.String, int, java.lang.String, java.lang.String)'
    }

    void testMultipleDefaultArgsWithMixedTypesAndWrongType() {
        shouldFailWithMessages '''
            String m(String first = 'first', int second, String third = 'third') {
                return first.toUpperCase() + ' ' + second + ' ' + third.toUpperCase()
            }
            m('hello') // no value set for "second"
        ''',
        '#m(java.lang.String)'
    }

    void testShouldNotFailWithAmbiguousMethodSelection() {
        assertScript '''
            StringBuffer sb = new StringBuffer()
            sb.append('foo')
        '''
    }

    void testShouldBeAbleToCallMethodUsingDoubleWithDoubleFloatLongIntShortOrByte() {
        assertScript '''
            double square(double x) { x*x }
            assert square(2.0d) == 4.0d
            assert square(2.0f) == 4.0d
            assert square(2L) == 4.0d
            assert square(2) == 4.0d
            assert square((short)2) == 4.0d
            assert square((byte)2) == 4.0d
        '''
    }

    void testShouldNotBeAbleToCallMethodUsingFloatWithDouble() {
        shouldFailWithMessages '''
            float square(float x) { x*x }
            assert square(2.0d) == 4.0d
        ''',
        '#square(double)'
    }

    void testShouldNotBeAbleToCallMethodUsingLongWithFloatOrDouble() {
        shouldFailWithMessages '''
            float square(long x) { x*x }
            assert square(2.0d) == 4.0d
            assert square(2.0f) == 4.0d
        ''',
        '#square(double)', '#square(float)'
    }

    void testShouldNotAllowMethodCallFromStaticInitializer() {
        shouldFailWithMessages '''
            class A {
                void instanceMethod() {}
                static {
                    instanceMethod()
                }
            }
            new A()
        ''',
        'Non-static method A#instanceMethod cannot be called from static context'
    }

    void testShouldNotAllowMethodCallFromStaticMethod() {
        shouldFailWithMessages '''
            class A {
                void instanceMethod() {}
                static void staticMethod() {
                    instanceMethod()
                }
            }
            A.staticMethod()
        ''',
        'Non-static method A#instanceMethod cannot be called from static context'
    }

    void testShouldNotAllowMethodCallFromStaticField() {
        shouldFailWithMessages '''
            class A {
                boolean instanceMethod() {}
                static FOO = instanceMethod()
            }
            new A()
        ''',
        'Non-static method A#instanceMethod cannot be called from static context'
    }

    // GROOVY-5495
    void testShouldFindMethodFromSuperInterface() {
        assertScript '''
            class ClassUnderTest {
                void methodFromString(SecondInterface si) {
                    si.methodFromSecondInterface();
                    si.methodFromFirstInterface();
                }
            }

            interface FirstInterface {
                void methodFromFirstInterface();
            }

            interface SecondInterface extends FirstInterface {
                void methodFromSecondInterface();
            }

            new ClassUnderTest()
        '''
    }

    void testShouldNotBeAmbiguousCall() {
        assertScript '''
            (0..10).find { int x -> x < 5 }
        '''
    }

    void testEqualsCalledOnInterface() {
        assertScript '''
            Serializable ser = (Serializable) new Integer(1)
            if (ser !=null) { // ser.equals(null)
                println 'ok'
                int hash = ser.hashCode()
                String str = ser.toString()
                try {
                    ser.notify()
                } catch (e) {}
                try {
                    ser.notifyAll()
                } catch (e) {}

                try {
                    ser.wait()
                } catch (e) {}
            }
        '''
    }

    // GROOVY-5534
    void testSafeDereference() {
        assertScript '''
            def foo() {
               File bar
               bar?.name
            }
            assert foo() == null
        '''
    }

    // GROOVY-5540
    void testChoosePublicMethodInHierarchy() {
        assertScript '''import groovy.transform.stc.MethodCallsSTCTest.Child2
            class A {
                int delegate() {
                    @ASTTest(phase=INSTRUCTION_SELECTION, value={
                        def md = node.rightExpression.getNodeMetaData(DIRECT_METHOD_CALL_TARGET)
                        assert md.declaringClass.nameWithoutPackage == 'MethodCallsSTCTest$ChildWithPublic'
                    })
                    int res = new Child2().m()
                    res
                }
            }
            assert new A().delegate() == 2
        '''
    }

    // GROOVY-5580
    void testGetNameAsPropertyFromSuperInterface() {
        assertScript '''
            interface Upper { String getName() }
            interface Lower extends Upper {}
            String foo(Lower impl) {
                impl.name // getName() called with the property notation
            }
            assert foo({ 'bar' } as Lower) == 'bar'
        '''
    }

    void testGetNameAsPropertyFromSuperInterfaceUsingConcreteImpl() {
        assertScript '''
            interface Upper { String getName() }
            interface Lower extends Upper {}
            class Foo implements Lower { String getName() { 'bar' } }
            String foo(Foo impl) {
                impl.name // getName() called with the property notation
            }
            assert foo(new Foo()) == 'bar'
        '''
    }

    void testGetNameAsPropertyFromSuperInterfaceUsingConcreteImplSubclass() {
        assertScript '''
            interface Upper { String getName() }
            interface Lower extends Upper {}
            class Foo implements Lower { String getName() { 'bar' } }
            class Bar extends Foo {}
            String foo(Bar impl) {
                impl.name // getName() called with the property notation
            }
            assert foo(new Bar()) == 'bar'
        '''
    }

    void testIsGetterAsPropertyFromSuperInterface() {
        assertScript '''
            interface Upper { boolean isBar() }
            interface Lower extends Upper {}
            boolean foo(Lower impl) {
                impl.bar // isBar() called with the property notation
            }
            assert foo({ true } as Lower)
        '''
    }

    void testIsGetterAsPropertyFromSuperInterfaceUsingConcreteImpl() {
        assertScript '''
            interface Upper { boolean isBar() }
            interface Lower extends Upper {}
            class Foo implements Lower { boolean isBar() { true } }
            boolean foo(Foo impl) {
                impl.bar // isBar() called with the property notation
            }
            assert foo(new Foo())
        '''
    }

    void testIsGetterAsPropertyFromSuperInterfaceUsingConcreteImplSubclass() {
        assertScript '''
            interface Upper { boolean isBar() }
            interface Lower extends Upper {}
            class Foo implements Lower { boolean isBar() { true } }
            class Bar extends Foo {}
            boolean foo(Bar impl) {
                impl.bar // isBar() called with the property notation
            }
            assert foo(new Bar())
        '''
    }

    // GROOVY-5580: getName variant
    void testGetNameFromSuperInterface() {
        assertScript '''
            interface Upper { String getName() }
            interface Lower extends Upper {}
            String foo(Lower impl) {
                impl.getName()
            }
            assert foo({ 'bar' } as Lower) == 'bar'
        '''
    }

    void testGetNameFromSuperInterfaceUsingConcreteImpl() {
        assertScript '''
            interface Upper { String getName() }
            interface Lower extends Upper {}
            class Foo implements Lower { String getName() { 'bar' } }
            String foo(Foo impl) {
                impl.getName()
            }
            assert foo(new Foo()) == 'bar'
        '''
    }

    void testGetNameFromSuperInterfaceUsingConcreteImplSubclass() {
        assertScript '''
            interface Upper { String getName() }
            interface Lower extends Upper {}
            class Foo implements Lower { String getName() { 'bar' } }
            class Bar extends Foo {}
            String foo(Bar impl) {
                impl.getName()
            }
            assert foo(new Bar()) == 'bar'
        '''
    }

    void testSpreadArgsForbiddenInNonStaticMethodCall() {
        shouldFailWithMessages '''
            def foo(String a, String b, int c, double d, double e) {
            }
            def bar(String[] strings, int i, double[] numbers) {
                foo(*strings, i, *numbers)
            }
        ''',
        'The spread operator cannot be used as argument of method or closure calls with static type checking because the number of arguments cannot be determined at compile time',
        'The spread operator cannot be used as argument of method or closure calls with static type checking because the number of arguments cannot be determined at compile time',
        'Cannot find matching method '
    }

    void testSpreadArgsForbiddenInStaticMethodCall() {
        shouldFailWithMessages '''
            static foo(String a, String b, int c, double d, double e) {
            }
            static bar(String[] strings, int i, double[] numbers) {
                foo(*strings, i, *numbers)
            }
        ''',
        'The spread operator cannot be used as argument of method or closure calls with static type checking because the number of arguments cannot be determined at compile time',
        'The spread operator cannot be used as argument of method or closure calls with static type checking because the number of arguments cannot be determined at compile time',
        'Cannot find matching method '
    }

    void testSpreadArgsForbiddenInConstructorCall() {
        shouldFailWithMessages '''
            class C {
                C(String a, String b) {
                }
            }
            new C(*['A','B'])
        ''',
        'The spread operator cannot be used as argument of method or closure calls with static type checking because the number of arguments cannot be determined at compile time',
        'Cannot find matching method C#<init>(java.util.List <E extends java.lang.Object>)'
    }

    void testSpreadArgsForbiddenInClosureCall() {
        shouldFailWithMessages '''
            def closure = { String a, String b, String c -> println "$a $b $c" }
            def strings = ['A','B','C']
            closure(*strings)
        ''',
        'The spread operator cannot be used as argument of method or closure calls with static type checking because the number of arguments cannot be determined at compile time'
    }

    void testBoxingShouldCostMore() {
        if (config.indyEnabled) return;
        assertScript '''
            int foo(int x) { 1 }
            int foo(Integer x) { 2 }

            @ASTTest(phase=INSTRUCTION_SELECTION, value={
                lookup('mce').each {
                    def call = it.expression
                    def target = call.getNodeMetaData(DIRECT_METHOD_CALL_TARGET)
                    assert target.parameters[0].type == int_TYPE
                }
            })
            int bar() {
                mce: foo(1)
            }
            bar()
            // commented out the next line because this is something
            // the dynamic runtime cannot ensure
            //assert bar() == 1
        '''
    }

    // GROOVY-5645
    void testSuperCallWithVargs() {
        assertScript '''
            class Base {
                int foo(int x, Object... args) { 1 }
                int foo(Object... args) { 2 }
            }
            class Child extends Base {
                void bar() {
                    assert foo(1, 'a') == 1
                    super.foo(1, 'a') == 1
                }
            }
            new Child().bar()
        '''
    }

    void testVargsSelection1() {
        assertScript '''
            int foo(int x, Object... args) { 1 }
            int foo(Object... args) { 2 }
            assert foo(1) == 1
            assert foo() == 2
            assert foo(1,2) == 1
        '''
    }

    void testVargsSelection2() {
        assertScript '''
            int sum(int x) { 1 }
            int sum(int... args) {
                0
            }
            assert sum(1) == 1
        '''
    }

    void testVargsSelection3() {
        assertScript '''
            int sum(int x) { 1 }
            int sum(int y, int... args) {
                0
            }
            assert sum(1) == 1
        '''
    }

    // GROOVY-6147
    void testVargsSelection4() {
        assertScript '''
            int select(Object a, String s) { 1 }
            int select(Object a, String s, Object[] args) { 2 }
            def o = new Date()
            def s = 'String'
            @ASTTest(phase=INSTRUCTION_SELECTION,value={
                def method = node.rightExpression.getNodeMetaData(DIRECT_METHOD_CALL_TARGET)
                assert method.name == 'select'
                assert method.parameters.length==2
            })
            def result = select(o,s)
            assert result == 1
        '''
    }

    // GROOVY-6195
    void testVargsSelection5() {
        assertScript '''
            def list = ['a', 'b', 'c']
            Object[] arr = list.toArray()
            println arr
        '''
    }

    // GROOVY-6235
    void testVargsSelection6() {
        assertScript '''import org.codehaus.groovy.classgen.asm.sc.support.Groovy6235SupportSub as Support
            def b = new Support()
            assert b.overload() == 1
            assert b.overload('a') == 1
            assert b.overload('a','b') == 2
        '''
    }

    // GROOVY-6646
    void testVargsSelection7() {
        assertScript '''
            def foo(Class... cs) { "Classes" }
            def foo(String... ss) { "Strings" }

            assert foo(List, Map) == "Classes"
            assert foo("2","1") == "Strings"
        '''
        assertScript '''
            def foo(Class<?>... cs) { "Classes" }
            def foo(String... ss) { "Strings" }

            assert foo(List, Map) == "Classes"
            assert foo("2","1") == "Strings"
        '''
    }

    // GROOVY-8737
    void testVargsSelection8() {
        String methods = '''
            String m(String key, Object[] args) {
                "key=$key, args=$args"
            }
            String m(String key, Object[] args, Object[] parts) {
                "key=$key, args=$args, parts=$parts"
            }
            String m(String key, Object[] args, String[] names) {
                "key=$key, args=$args, names=$names"
            }
        '''
        assertScript methods + '''
            String result = m( 'hello', ['world'] as Object[] ) // exact match for m(String,Object[])
            assert result == 'key=hello, args=[world]'
        '''
        assertScript methods + '''
            String result = m( 'hello', ['world'] as String[] )
            assert result == 'key=hello, args=[world]'
        '''
        assertScript methods + '''
            String result = m( "${'hello'}", 'world' )
            assert result == 'key=hello, args=[world]'
        '''
        assertScript methods + '''
            String result = m( 'hello', 'world' )
            assert result == 'key=hello, args=[world]'
        '''

        assertScript methods + '''
            String result = m( 'hello', ['there'] as String[], 'Steve' )
            assert result == 'key=hello, args=[there], names=[Steve]'
        '''
    }

    // GROOVY-5702
    void testShouldFindInterfaceMethod() {
        assertScript '''
            interface OtherCloseable {
                void close()
            }

            abstract class MyCloseableChannel implements OtherCloseable {  }

            class Test {
                static void test(MyCloseableChannel mc) {
                    mc?.close()
                }
            }

            Test.test(null)
        '''
    }

    void testShouldFindInheritedInterfaceMethod() {
        assertScript '''
            interface Top { void foo() }
            interface Middle extends Top {}
            interface Bottom extends Middle {}

            void test(Bottom b) {
               b.foo()
            }
        '''
    }

    void testShouldFindInheritedInterfaceMethod2() {
        assertScript '''
            interface Top { int foo(int i) }
            interface Middle extends Top { int foo(String s) }
            interface Bottom extends Middle {}

            void test(Bottom b) {
                b.foo(123)
            }
        '''
    }

    void testShouldFindInheritedInterfaceMethod3() {
        assertScript '''
            interface Top { int foo(int i) }
            interface Middle extends Top { }
            interface Bottom extends Middle { int foo(String s) }

            void test(Bottom b) {
                b.foo(123)
            }
        '''
    }

    void testShouldFindInheritedInterfaceMethod4() {
        assertScript '''
            interface Top { int foo(int i) }
            interface Middle extends Top { int foo(String s) }
            abstract class Bottom implements Middle {}

            int test(Bottom b) {
                b.foo(123)
            }
            def bot = new Bottom() {
                int foo(int i) { 1 }
                int foo(String s) { 2 }
            }
            assert test(bot) == 1
        '''
    }

    void testShouldFindInheritedInterfaceMethod5() {
        assertScript '''
            interface Top { int foo(int i) }
            interface Middle extends Top { }
            abstract class Bottom implements Middle { abstract int foo(String s) }

            int test(Bottom b) {
                b.foo(123)
            }
            def bot = new Bottom() {
                int foo(int i) { 1 }
                int foo(String s) { 2 }
            }
            assert test(bot) == 1
        '''
    }

    // GROOVY-9890
    void testShouldFindInheritedInterfaceDefaultMethod() {
        if (IS_PRE_8) return

        def parentDir = File.createTempDir()
        config.targetDirectory = File.createTempDir()
        config.jointCompilationOptions = [stubDir: File.createTempDir()]
        try {
            def a = new File(parentDir, 'Face.java')
            a.write '''
                public interface Face {
                    default Object foo(long n) {
                        return n;
                    }
                    Object foo(String s);
                }
            '''
            def b = new File(parentDir, 'Impl.groovy')
            b.write '''
                class Impl implements Face {
                    @Override def foo(String s) {
                        return s
                    }
                    // def foo(long n)
                }
            '''
            def c = new File(parentDir, 'Main.groovy')
            c.write '''
                def result = new Impl().foo(42L)
                assert result.class == Long.class
            '''

            def loader = new GroovyClassLoader(this.class.classLoader)
            def cu = new JavaAwareCompilationUnit(config, loader)
            cu.addSources(a, b, c)
            cu.compile()

            loader.loadClass('Main').main()
        } finally {
            parentDir.deleteDir()
            config.targetDirectory.deleteDir()
            config.jointCompilationOptions.stubDir.deleteDir()
        }
    }

    // GROOVY-5743
    void testClosureAsParameter() {
        assertScript '''
            Integer a( String s, Closure<Integer> b ) {
                b( s )
            }

            assert a( 'tim' ) { 0 } == 0
        '''
    }

    // GROOVY-5743
    void testClosureAsParameterWithDefaultValue() {
        assertScript '''
            Integer a( String s, Closure<Integer> b = {String it -> it.length()}) {
                b( s )
            }

            assert a( 'tim' ) == 3
        '''
    }

    // GROOVY-5712
    void testClassForNameVsCharsetForName() {
        assertScript '''import java.nio.charset.Charset
            Charset charset = Charset.forName('UTF-8')
            assert charset instanceof Charset
        '''
    }

    // GROOVY-10939
    void testClassHashCodeVsObjectHashCode() {
        assertScript '''
            int h = this.getClass().hashCode()
        '''
    }

    // GROOVY-5810
    void testCallStaticSuperMethod() {
        assertScript '''
            class Top {
                static boolean called = false
                public static foo() {
                    called = true
                }
            }

            class Bottom extends Top {
                public static foo() {
                    super.foo()
                }
            }
            Bottom.foo()
            assert Top.called
        '''
    }

    void testShouldFindSetProperty() {
        assertScript '''
            class A {
                int x
                void foo() {
                    this.setProperty('x', 1)
                }
            }
            def a = new A()
            a.foo()
            assert a.x == 1
        '''
    }

    // GROOVY-5888
    void testStaticContextScoping() {
        assertScript '''
            class A {
                static List foo = 'a,b,c'.split(/,/)*.trim()
            }
            assert A.foo == ['a','b','c']
        '''
    }

    // GROOVY-10720
    void testOverloadedMethodWithArray() {
        assertScript '''
            static Object m(double[] array) {
                array[0]
            }
            static <T> T m(T[] array) {
                array[0]
            }

            Double[] array = [ Double.valueOf(1) ]
            double value = m(array).doubleValue()
            assert value == 1d
        '''

        if (IS_PRE_8) return

        assertScript '''
            Double[] array = new Double[1]
            def stream = Arrays.stream(array) // stream(T[])
            assert stream.map{d -> 'string'}.findFirst().get() == 'string'
        '''
    }

    // GROOVY-5883, GROOVY-6270
    void testClosureUpperBound() {
        assertScript '''
            class Test<T> {
                def map(Closure<T> mapper) { 1 }
                def m1(Closure<Boolean> predicate) {
                    map { T it -> return predicate(it) ? it : null }
                }
                def m2(Closure<Boolean> predicate) {
                    map { T it -> return predicate(it) ? it : (T) null }
                }
                def m3(Closure<Boolean> predicate) {
                    Closure<T> c = { T it -> return predicate(it) ? it : null }
                    map(c)
                }
            }
            def t = new Test<String>()
            assert t.m1{true} == 1
            assert t.m2{true} == 1
            assert t.m3{true} == 1
        '''
    }

    // GROOVY-6569, GROOVY-6528
    void testMoreExplicitErrorMessageOnStaticMethodNotFound() {
        shouldFailWithMessages '''
            Double.isFiniteMissing(2.0d)
        ''',
        'Cannot find matching method java.lang.Double#isFiniteMissing(double)'

        shouldFailWithMessages '''
            String.doSomething()
        ''',
        'Cannot find matching method java.lang.String#doSomething()'
    }

    // GROOVY-6776
    void testPrimtiveParameterAndNullArgument() {
        shouldFailWithMessages '''
            def foo(int i){}
            def bar() {
                foo null
            }
            bar()
        ''',
        '#foo(int) with arguments [<unknown parameter type>]'
    }

    // GROOVY-6751
    void testMethodInBothInterfaceAndSuperclass() {
        assertScript '''
            interface Ifc {
              Object getProperty(String s)
            }

            class DuplicateMethodInIfc implements Ifc {}  // implemented in groovy.lang.GroovyObject

            class Tester {
              DuplicateMethodInIfc dup = new DuplicateMethodInIfc()
              Object obj = dup.getProperty("foo")
            }

            try { new Tester()}
            catch(groovy.lang.MissingPropertyException expected) {}
        '''
    }

    // GROOVY-7987
    void testNonStaticMethodViaStaticReceiver() {
        shouldFailWithMessages '''
            class Foo {
                def m() {}
            }
            Foo.m()
        ''',
        'Non-static method Foo#m cannot be called from static context'
    }

    // GROOVY-7813
    void testNonStaticOuterMethodCannotBeCalledFromStaticClass() {
        shouldFailWithMessages '''
            class Foo {
                def m() {}
                static class Bar {
                    void test() { m() }
                }
            }
        ''',
        'Non-static method Foo#m cannot be called from static context'
    }

    void testStaticOuterMethodCanBeCalledFromStaticClass() {
        assertScript '''
            class Foo {
                static def sm() { 2 }
                static class Bar {
                    void test() {
                        assert sm() == 2
                    }
                }
            }
            new Foo.Bar().test()
        '''
    }

    void testInheritedMethodCanBeCalledFromStaticClass() {
        assertScript '''
            class Foo {
                def m() { 1 }
            }

            class Bar {
                static class Baz extends Foo {
                    void test() {
                        assert m() == 1
                    }
                }
            }
            new Bar.Baz().test()
        '''
    }

    // GROOVY-8445
    void testCoerceClosureToFunctionalInterface1() {
        if (IS_PRE_8) return
        assertScript '''
            class Main {
                static main(args) {
                    assert 13 == [1, 2, 3].stream().reduce(7, {Integer r, Integer e -> r + e})
                }
            }
        '''
    }

    // GROOVY-8138
    void testCoerceClosureToFunctionalInterface2() {
        if (IS_PRE_8) return
        assertScript '''import static java.util.stream.Collectors.toList
            class Main {
                static main(args) {
                    List<String> tables = ['a', 'b']
                    assert ['a'] == tables.stream().filter { tableName -> tableName.contains('a') }.collect(toList())
                }
            }
        '''
    }

    @groovy.transform.NotYetImplemented
    void testCoerceReturnValueToMethodReturnType() {
        assertScript '''
            enum E { FOO, BAR }

            E[] m() {
                return E.values().toList() // values() returns an array...
            }
        '''
    }

    //--------------------------------------------------------------------------

    static class MyMethodCallTestClass {
        static String echo(String msg) {
            msg
        }
        static int mul(int... ints) {
            ints.toList().inject(1) { x,y -> x*y }
        }

        int add(int x, int y) { x+y }
        int add(double x, double y) { 2*x+y }
        int sum(int... args) { args.toList().sum() }
    }

    static class MyMethodCallTestClass2<T> extends MyMethodCallTestClass {
        T[] identity(T... args) { args }
    }

    static class MyMethodCallTestClass3 extends MyMethodCallTestClass2<String> {
    }

    static class GroovyPage {
        final void printHtmlPart(int partNumber) {}
        final void createTagBody(int bodyClosureIndex, Closure<?> bodyClosure) {}
    }

    static class BaseWithProtected {
        protected int m() { 1 }
    }

    static class ChildWithPublic extends BaseWithProtected {
        int m() { 2 }
    }

    static class Child2 extends ChildWithPublic {
    }
}
