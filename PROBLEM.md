# Beorn SSR

### Context: Server Side Rendering
[Liferay Portal]( https://github.com/liferay/liferay-portal "Liferay Portal") uses JSP+JSTL+Freemarker to provide Server Side Rendering.
However, you can integrate your own tool instead. For instance, Adobe Experience Manager
introduces the HTML Template Language ([HTL]( https://docs.adobe.com/docs/en/htl/overview.html "HTL")).

The main idea is that the server-side template is already a valid HTML. The usual operations of conditional 
and loop are written in HTML5's data-attributes.

### The exercise: Beorn SSR
For this exercise, you will implement something similar to HTL, called `"Beorn SSR"`.
It is a small server-side HTML templating language with some server-side scripting in Javascript.

You need to know Java, servlets, HTML5, and Javascript. You do not need to know any Liferay or AEM.

The aim is to have a template file like the following on the web server (template `index.html` already provided):

```html
<!DOCTYPE html>
<html>
 <script type="server/javascript"> 
  importClass(Packages.com.beorntech.Person) 
  var id = request.getParameter("id");
  var person = Person.lookup(id);
 </script>
 <head>
  <title>${person.name}</title> 
 </head>
 <body>
  <h1 title="${person.name}">${person.name}</h1>
  <h2 data-if="person.married" title="${person.spouse}">
    Spouse: ${person.spouse}
  </h2>
  <div data-for-child="person.children">
    Child: ${child}
  </div> 
 </body>
</html>
```
Then, a provided Person Java class:

```java
package com.beorntech; 

public class Person {
  public static Person lookup(String id) { return ...; }
  public String getName() {...}
  public boolean isMarried() {...} 
  public String getSpouse() {...}
  public List<String> getChildren() {...} 
} 
```
renders an HTML page like:

```html
<!DOCTYPE html>
<html>
 <head>
  <title>Maria</title>
 </head>
 <body>
  <h1 title="Maria">Maria</h1>
  <h2 title="Pierre">Spouse: Pierre</h2>
  <div>Child: Anne</div>
  <div>Child: Emma</div>
  <div>Child: Lina</div>
 </body>
</html>
```

You will write a java servlet that responds to requests for HTML files. The servlet reads the requested file, parses the HTML, and processes the server-side javascript and server-side data-attributes, and finally sends the resulting HTML to the browser.

**Please, use the provided `.zip` file as starting point.
The solution should provide testing URL similar to http://localhost:8080/index.html?id=2**

### Specification

#### < script > element

A server-side script element has the attribute `<script type="server/javascript">`. 
If the servlet finds such a script element, it should execute its body as javascript. 
The element itself produces no output for the browser.

For javascript-execution you may use Java's built-in Javascript engine. Java 7 includes a version of Mozilla's Rhino engine. Java 8 has a similar Javascript engine called `Nashorn`. You may also include Mozilla's Rhino engine.

The script element may put new javascript variables in the state, like `person` in the example. Initially, the state should have at least one useful variable: `request`, being the current `HttpServletRequest object`. Using that, the template can depend on input from URL parameters.

It is possible to call Java classes from the javascript in the element. The details on how it exactly works depend on which Javascript engine you use. In general, there will be facilities to import Java packages and classes, instantiate Java classes, and call methods on them, with a close correspondence between the Javascript syntax and Java syntax. All three mentioned Javascript engines know the getter-setter pattern. So the javascript `person.name` will be mapped to the Java methods `getName()` or `setName(String)` in the usual way.


#### $-expressions
There may be server-side `$-expressions` in template. They may appear inside the element bodies and inside the values of attributes. That means they may not appear in the element tags or in the keys of attributes.

When the servlet finds an expression, it should evaluate the javascript expression in the current javascript state, and render the result in the HTML output.

If the result is rendered inside the body of an HTML element, you must escape any characters that are not allowed inside an HTML element. If the result is rendered inside the value of an attribute, you must escape any characters that are not allowed inside a html attribute value.

#### data-if
An HTML element may have an attribute with key `data-if`.

If the result is `true`, the servlet should render the element in the normal way, but without the `data-if` attribute. 

If the result is `false`, the servlet should not render the element.
#### data-for-x
An HTML element may have an attribute like `data-for-x`, where `x` stands for a something that can be a variable name in javascript. When the servlet finds such an attribute, it should evaluate the attribute value as a javascript expression, in the current javascript state. If the result is an array (or a similar thing), then the servlet should render the element once for every item in the array. During each rendering of the element, the variable x stands for the item in the array. The result of the javascript evaluation may come from a java method call, therefore objects similar should also work:
* javascript array 
* java array 
* java collections

If an element has both data-if and data-for attributes, what should happen? What do you think is most useful?

### Implementation hints
You should be able to run the servlet with the provided package using mvn jetty:run. For parsing HTML, you can use any built-in library, your own code, or any libraries you like, as long as they are open source and you add them to the project. Make your life easy: You may assume all HTML files are valid XML. Having parsed HTML, the servlet should traverse the HTML elements in tree-order, and render them, in order to send them to the browser. The formatted HTML needn't be the same character-by-character, as long as it is equivalent as HTML.
### Optional: local variables
How could we add local variables? Maybe I want variable `person` to stand for the value of `new Person("Maria")`, but only during the rendering of one `<div>-element`. After that element, the variable should go back to whatever value it had before if it had any.
### Optional: inclusion
How could one template include another? Can we use the new html5 `template-element`? Can there be a `data-attribute` that includes another template?
### In general, things you may need:
* Good Object Oriented design is a must. Badly structured code is our main reason for rejection.
* Provide description. Document your design. Argue for it. Mention any alternatives that you have discarded.
* You may use libraries if they are open-source and you include them.
* You can develop in any IDE that you find appropriate (we'll just look into the code). For example, Eclipse, IntelliJ, ... Use Maven.
* Use Java 8.
* Clean code, please.
* Tests that demonstrate that the code is correct are highly recommended.
* Your code should be well documented.
* You may use Google and any other sources of information, of course.

