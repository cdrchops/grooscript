/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.grooscript.convert

import org.grooscript.test.ConversionMixin
import org.grooscript.test.JsTestResult

class TestAdvancedClass extends GroovyTestCase implements ConversionMixin {

    def 'test class names' () {
        expect:
        convertAndEvaluate('classes/Names')
    }

    def 'test instanceof basic'() {
        expect:
        convertAndEvaluate('classes/InstanceOf')
    }

    def 'add methods and properties to classes'() {
        expect:
        convertAndEvaluate('classes/AddingStuff')
    }

    def 'who knows categories'() {
        expect:
        convertAndEvaluate('classes/Categories')
    }

    def 'mixins to the hell'() {
        expect:
        convertAndEvaluate('classes/Mixins')
    }

    def 'string buffer'() {
        when:
        JsTestResult result = convertAndEvaluateWithJsEngine('classes/StringBufferClass')

        then:
        result.console == 'hello!'
        !result.assertFails
    }

    def 'abstract class basic usage'() {
        expect:
        convertAndEvaluate('classes/Abstract')
    }

    def 'using @Category'() {
        expect:
        convertAndEvaluate('classes/AddCategories')
    }

    def 'using supported types'() {
        expect:
        convertAndEvaluate('classes/SupportedTypes')
    }

    def 'using as keyword'() {
        expect:
        convertAndEvaluate('classes/AsKeyword')
    }

    def 'using primitive arrays'() {
        expect:
        convertAndEvaluate('classes/PrimitiveArrays')
    }

    def 'starting @Delegate'() {
        expect:
        convertAndEvaluate('classes/StartingDelegate')
    }

    def 'more categories'() {
        expect:
        convertAndEvaluate('classes/MoreCategories')
    }

    def 'date functions'() {
        expect:
        convertAndEvaluate('classes/DateClass')
    }

    def 'default method call'() {
        expect:
        convertAndEvaluate('classes/DefaultMethodCall')
    }

    def 'test inner classes'() {
        expect:
        convertAndEvaluate('classes/Inner')
    }

    def 'test static properties'() {
        expect:
        convertAndEvaluate('classes/StaticProperties')
    }

    def 'test static methods'() {
        expect:
        convertAndEvaluate('classes/StaticMethods')
    }

    def 'test this uses'() {
        expect:
        convertAndEvaluate('classes/This')
    }
}
