# laser

[![Build Status](https://secure.travis-ci.org/Raynes/laser.png)](http://travis-ci.org/Raynes/laser)

[API reference](http://raynes.github.com/laser/)

Laser is an HTML transformation library. I wouldn't call it a templating
library, but that's the purpose it'll likely be used for the most and it is well
suited to the task.

Laser is similar to [Enlive](https://github.com/cgrand/enlive) and
[Tinsel](https://github.com/davidsantiago). Like those libraries, the idea is to
work with plain HTML with no special markup. You take that plain HTML in and use
selectors to select pieces of HTML and transformation functions to transform the
HTML the way you want. Laser comes with a bunch of selectors and transformers
built in.

Huge props to David Santiago for writing
[hickory](https://github.com/davidsantiago/hickory) and helping me out with zippers.

## WHYYYYYYYYY!?!?!

I wrote laser for a couple of reasons.

* Enlive is HUGE! Over 800 lines. Laser is around 300 right now.
* Enlive does its job, but it is extremely complex, has terrible
  documentation, and at this point in time seems to be hardly maintained at all.
* Enlive currently uses tagsoup which is ew compared to jsoup.
* I prefer function-based selectors rather than faux css selectors.
* Tinsel is really nice, but one of my specific use-cases is a one-off runtime
  transformation and tinsel isn't designed for that sort of thing (though it
  could do it).
* I like writing libraries so I can do really fun and crazy things with them
  that make me and other people happy. I also get to bitch about having too many
  things to maintain.

## Usage

Laser is designed around selectors and transformers and combinators for
them. It's pretty easy. Let's put together some HTML to transform:

```html
<html>
  <head></head>
  <body>
    <p>foo</p>
    <p id="hi">bar</p>
    <div>
      <p class="meow">baz</p>
    </div>
  </body>
</html>
```

Let's get that in a Clojure string.

```clojure
(def html "<html><head></head><body><p>foo</p><p id=\"hi\">bar</p><div><p class=\"meow\">baz</p></div></body></html>")
```

Now, let's try some transformations.

```clojure
user> (laser/document (laser/parse html) (laser/element= :p) (laser/content "omg"))
"<html><head></head><body><p>omg</p><p id=\"hi\">omg</p><div><p class=\"meow\">omg</p></div></body></html>"
```

Easy enough, right? This transforms our HTML document to make all `p` tags have
`"omg` content. Everybody needs this, right? It's important.

But darn it, we don't want all of our `p` to be omg. Let's only change the ones
with the `meow` class!

```clojure
user> (laser/document (laser/parse html) (laser/class= "meow") (laser/content "omg"))
"<html><head></head><body><p>foo</p><p id=\"hi\">bar</p><div><p class=\"meow\">omg</p></div></body></html>"
```

Great! How about if we only want to transform the one with id "hi"?

```clojure
user> (laser/document (laser/parse html) (laser/id= "hi") (laser/content "omg"))
"<html><head></head><body><p>foo</p><p id=\"hi\">omg</p><div><p class=\"meow\">baz</p></div></body></html>"
```

How about we transform the one with id "hi" and the ones with class "meow" at
the same time? WILD!

```clojure
user> (laser/document (laser/parse html) (laser/id= "hi") (laser/content "omg") (laser/class= "meow") (laser/content "omg"))
"<html><head></head><body><p>foo</p><p id=\"hi\">omg</p><div><p class=\"meow\">omg</p></div></body></html>"
```

Ermahgerd.

That's pretty simple, right? You might run into a little issue if you try to use
this on a piece of HTML instead of a full document.

```clojure
user> (laser/document (laser/parse "<p>hi</p>") (laser/element= :p) (laser/content "omg"))
"<html><head></head><body><p>omg</p></body></html>"
```

WAAAAAAT!?!?! There are html, head, and body tags, but we didn't put them there!
I'm kinda angry now. The problem here is that anything in laser with `document`
in the name works with entire HTML documents. If you don't give it a complete
document, it makes it one.

The idea you can have a bunch of 'fragment' of HTML that you process, and then
use that HTML in a top-level document transformation.

```clojure
user> (laser/fragment (laser/parse-fragment "<p>hi</p>") (laser/element= :p) (laser/content "omg"))
({:type :element, :attrs nil, :tag :p, :content ["omg"]})
```

An HTML string is not returned by `fragment`. The reasoning for this is because
you will typically pass fragments to other templates, splicing it in with the
`html-content` tranformation. If you do want an HTML string just do this:

```clojure
user> (laser/fragment-to-html (laser/fragment (laser/parse-fragment "<p>hi</p>") (laser/element= :p) (laser/content "omg")))
"<p>omg</p>"
```

Let's try combining a fragment and a document.

```clojure
user> (laser/document
        (laser/parse html)
        (laser/element= :body) (-> "<p>foo</p>"
                                   (laser/parse-fragment)
                                   (laser/fragment
                                   (laser/element= :p) (laser/content "no, it's a bar!"))
                                   (laser/html-content)))
"<html><head></head><body><p>no, it's a bar!</p></body></html>"
```

Now that's pretty cool, right? This example takes our HTML from earlier and
replaces the body with HTML from a fragment. The fragment is a single `p`
tag. All we're doing is replacing its text with some other text, just to make
the example worthwhile.

Finally, we also have some simple convinence macros for defining documents and
fragments. Let's use them to do the same thing we did above a little prettier.

```clojure
user> (laser/defragment i-gotta-pee "<p>foo</p>" [] (laser/element= :p) (laser/content "no, it's a bar!"))
#'user/i-gotta-pee
user> (laser/defdocument i-like-fries html [] (laser/element= :body) (laser/html-content (i-gotta-pee)))
#'user/i-like-fries
user> (i-like-fries)
"<html><head></head><body><p>no, it's a bar!</p></body></html>"
```

These macros don't do much at all so you don't have to use them unless they're a
better fit for what you're doing. The most important thing is that the functions
they define do not parse the HTML they're passed every single time. The HTML is
parsed once as soon as the function is defined. This has the effect that if
you're using HTML in files, you'll need to reload the namespace with your
templates before you see the changes.

### Advanced selecting

Our examples are fun, but they don't do anything terribly interesting. Laser has
a bunch of combinators to do interesting and complicated things with
selectors. Here is one example. What if we wanted to only match `p` tags inside
of a `div` and then generate a bunch of new p tags?

```clojure
user> (laser/fragment-to-html (laser/fragment html (laser/child-of (laser/element= :div) (laser/element= :p)) (fn [node] (for [x (range 10)] (assoc node :content [(str x)])))))
"<div><p>0</p><p>1</p><p>2</p><p>3</p><p>4</p><p>5</p><p>6</p><p>7</p><p>8</p><p>9</p></div>"
```

```clojure
user> (laser/document (laser/parse html) (laser/child-of (laser/element= :div) (laser/element= :p)) (laser/content "omg"))
"<html><head></head><body><p>foo</p><p id=\"hi\">bar</p><div><p class=\"meow\">omg</p></div></body></html>"
```

There are a bunch of combinators. Take a look at the [API reference](http://raynes.github.com/laser/) or code
for a full list.

### Selectors and transformers.

Selectors and transformers are both just functions. All the ones we've shown are just
functions that return functions that take nodes and return a node or a seq of
nodes. Let's take a look at how the `element=` selector is defined.

```clojure
(defn element=
  "A selector that matches an element with this name."
  [element]
  (fn [loc] (= element (-> loc zip/node :tag))))
```

`element=` is a function that takes an element and returns a selector function
that matches when the element of the html node you're currently at is the same
as the one passed into it. Under the hood, laser works with zippers. Selectors
are passed the current zipper location and then the selector can do whatever it
wants with it. Nodes inside the zipper are maps of the
[hickory](https://github.com/davidsantiago/hickory) format, similar to
clojure.xml.

Transformations are similar. Let's look at `content`

```clojure
(defn content
  "Set content of node to s."
  [s]
  (fn [node] (assoc node :content [(escape-html s)])))
```

`content` is a function that takes a string and returns a function that takes an
html node (hickory format, as always) and sets the content of it to the string
after it has been escaped.

Given this knowledge, all the combinators (`select-and`, `select-or`,
`child-of`, etc) are all just specialized ways of composing selectors! Simple
enough, right?

You can write your own selectors and transformers, but if you write any that you
use often or think are generally useful, throw 'em at me in a pull request.

### Screen Scraping

You can also use laser for screen scraping. It has a `select` function
specifically for this purpose:

```clojure
me.raynes.laser=> (select (parse "<div><a id=\"hi\">hi</a></div>") (id= "hi"))
({:type :element, :attrs {:id "hi"}, :tag :a, :content ["hi"]})
me.raynes.laser=> (select (parse "<div><a id=\"hi\">hi</a><a>bye</a></div>") (element= :a))
({:type :element, :attrs {:id "hi"}, :tag :a, :content ["hi"]} {:type :element, :attrs nil, :tag :a, :content ["bye"]})
```

### Advanced Transforming

You can do some pretty fancy things with transformers. Our examples are fun, but
they don't do anything terribly interesting. Laser has
a bunch of combinators to do interesting and complicated things with
selectors. Here is one example. What if we wanted to only match `p` tags inside
of a `div` and then generate a bunch of new p tags there?

```clojure
user> (laser/fragment-to-html
        (laser/fragment
          html
          (laser/child-of (laser/element= :div)
                          (laser/element= :p)) (fn [node]
                                                 (for [x (range 10)]
                                                   (assoc node :content [(str x)])))))
"<div><p>0</p><p>1</p><p>2</p><p>3</p><p>4</p><p>5</p><p>6</p><p>7</p><p>8</p><p>9</p></div>"
```

Nice, huh?

## Performance

I haven't done much benchmarking. All I have done so far is clone David
Santiago's view benchmarking stuff (which is specifically for this purpose),
added laser to it and ran it against tinsel, hiccup, raw strings, and
Enlive. Here are my results:

```
hiccup
"Elapsed time: 86.974 msecs"
"Elapsed time: 77.422 msecs"
"Elapsed time: 84.522 msecs"
hiccup (type-hint)
"Elapsed time: 36.095 msecs"
"Elapsed time: 45.961 msecs"
"Elapsed time: 42.489 msecs"
str
"Elapsed time: 3.627 msecs"
"Elapsed time: 2.099 msecs"
"Elapsed time: 1.967 msecs"
enlive
"Elapsed time: 47.944 msecs"
"Elapsed time: 36.506 msecs"
"Elapsed time: 36.081 msecs"
enlive with snippet
"Elapsed time: 72.872 msecs"
"Elapsed time: 62.419 msecs"
"Elapsed time: 69.803 msecs"
tinsel
"Elapsed time: 81.255 msecs"
"Elapsed time: 84.682 msecs"
"Elapsed time: 69.489 msecs"
tinsel (type-hint)
"Elapsed time: 38.558 msecs"
"Elapsed time: 57.279 msecs"
"Elapsed time: 41.377 msecs"
laser
"Elapsed time: 28.211 msecs"
"Elapsed time: 28.341 msecs"
"Elapsed time: 23.837 msecs"
laser (type-hint)
"Elapsed time: 28.592 msecs"
"Elapsed time: 26.309 msecs"
"Elapsed time: 26.148 msecs"
```

My benchmarks used `defdocument`.

What does this mean? Not the slightest clue. I haven't really done anything
special for performance, and tinsel has some nice compile-time optimizations
that make it do as much as possible at compile-time, so I imagine it is faster
in some scenarios. The templates in the benchmark also seem fairly trivial, so I
don't really know how they measure up with large templates and complex
selecting/transforming. I think they are all close enough that the most
important thing is using what you like the most.

## TODO

Biggest TODO at the moment is a function for turning a string representing a CSS
selector into a selector function you can use (by combining existing selectors).

## Credits

* Anthony Grimes - The author.
* David Santiago - Huge part of laser relies on his library Hickory for HTML
  stuff.
* Andrew Brehaut - Uses Enlive, likes laser, gave me ideas.

## License

Copyright © 2012 Anthony Grimes

Distributed under the Eclipse Public License, the same as Clojure.
