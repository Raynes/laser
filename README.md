# laser

[![Build Status](https://secure.travis-ci.org/Raynes/laser.png)](http://travis-ci.org/Raynes/laser)

[API reference](http://raynes.github.com/laser/)

I wrote a fairly large and thorough guide to laser
[here](https://github.com/Raynes/laser/blob/master/docs/guide.md), but there is
also a simple example below to wet your whistle if you'd like.

Check the [changelog](https://github.com/Raynes/laser/blob/master/CHANGELOG.md)
for changes between versions.

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

## Example

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

That's pretty simple, right? Laser can do a *lot* more than this. Please read
the full (and fairly massive) guide to laser at https://github.com/Raynes/laser/blob/master/docs/guide.md

## Performance

I haven't done much benchmarking. All I have done so far is clone David
Santiago's view benchmarking stuff (which is specifically for this purpose),
added laser to it and ran it against tinsel, hiccup, raw strings, and
Enlive. Here are my results:

```
hiccup
"Elapsed time: 73.931 msecs"
"Elapsed time: 74.661 msecs"
"Elapsed time: 67.882 msecs"
hiccup (type-hint)
"Elapsed time: 44.973 msecs"
"Elapsed time: 34.207 msecs"
"Elapsed time: 30.914 msecs"
str
"Elapsed time: 2.956 msecs"
"Elapsed time: 2.238 msecs"
"Elapsed time: 1.827 msecs"
enlive
"Elapsed time: 27.871 msecs"
"Elapsed time: 30.446 msecs"
"Elapsed time: 27.742 msecs"
enlive with snippet
"Elapsed time: 46.826 msecs"
"Elapsed time: 46.143 msecs"
"Elapsed time: 43.042 msecs"
tinsel
"Elapsed time: 57.633 msecs"
"Elapsed time: 56.65 msecs"
"Elapsed time: 46.488 msecs"
tinsel (type-hint)
"Elapsed time: 27.126 msecs"
"Elapsed time: 35.935 msecs"
"Elapsed time: 33.945 msecs"
laser
"Elapsed time: 27.078 msecs"
"Elapsed time: 27.828 msecs"
"Elapsed time: 23.844 msecs"
laser (type-hint)
"Elapsed time: 25.186 msecs"
"Elapsed time: 24.318 msecs"
"Elapsed time: 25.126 msecs"
```

My benchmarks used `defdocument`.

What does this mean? Not the slightest clue. I haven't really done anything
special for performance, and tinsel has some nice compile-time optimizations
that make it do as much as possible at compile-time, so I imagine it is faster
in some scenarios. The templates in the benchmark also seem fairly trivial, so I
don't really know how they measure up with large templates and complex
selecting/transforming. I think they are all close enough that the most
important thing is using what you like the most.

## Credits

* Anthony Grimes - The author.
* David Santiago - Huge part of laser relies on his library Hickory for HTML
  stuff.
* Andrew Brehaut - Uses Enlive, likes laser, gave me ideas.

## License

Copyright © 2012 Anthony Grimes

Distributed under the Eclipse Public License, the same as Clojure.
