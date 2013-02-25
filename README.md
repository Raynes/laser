# laser

[![Build Status](https://secure.travis-ci.org/Raynes/laser.png)](http://travis-ci.org/Raynes/laser)

[API reference](http://raynes.github.com/laser/docs)

I wrote a fairly large and thorough guide to laser
[here](https://github.com/Raynes/laser/blob/master/docs/guide.md), but there is
also a simple example below to whet your whistle if you'd like.

Check the [changelog](https://github.com/Raynes/laser/blob/master/CHANGELOG.md)
for changes between versions.

Laser is an HTML transformation library. I wouldn't call it a templating
library, but that's the purpose it'll likely be used for the most and it is well
suited to the task.

Laser is similar to [Enlive](https://github.com/cgrand/enlive) and
[Tinsel](https://github.com/davidsantiago/tinsel). Like those libraries, the idea is to
work with plain HTML with no special markup. You take that plain HTML in and use
selectors to select pieces of HTML and transformation functions to transform the
HTML the way you want. Laser comes with a bunch of selectors and transformers
built in.

Huge props to David Santiago for writing
[hickory](https://github.com/davidsantiago/hickory) and helping me out with zippers.

## WHYYYYYYYYY!?!?!

I wrote laser for a couple of reasons.

* Enlive does its job and is the precursor to the way laser does things. However,
  it is very large and (arguably?) complex compared to laser. laser strives to be as
  simple as possible.
* Enlive historically used tagsoup and I wanted something backed by jsoup.
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

Now, let's try some transformations. `laser/document` takes a parsed document
and alternating selector and transformer arguments:

```clojure
user> (laser/document (laser/parse html) (laser/element= :p) (laser/content "omg"))
"<html><head></head><body><p>omg</p><p id=\"hi\">omg</p><div><p class=\"meow\">omg</p></div></body></html>"
```

Easy enough, right? This transforms our HTML document to make all `p` tags have
`"omg"` content. Everybody needs this, right? It's important.

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
user> (laser/document (laser/parse html)
        (laser/id= "hi")      (laser/content "omg")
        (laser/class= "meow") (laser/content "omg"))
"<html><head></head><body><p>foo</p><p id=\"hi\">omg</p><div><p class=\"meow\">omg</p></div></body></html>"
```

Ermahgerd.

That's pretty simple, right? Laser can do a *lot* more than this. Please read
the full (and fairly massive) guide to laser at https://github.com/Raynes/laser/blob/master/docs/guide.md

## Credits

* Anthony Grimes - The author.
* David Santiago - Huge part of laser relies on a fork of his Hickory lib for HTML
  stuff.
* Andrew Brehaut - Uses Enlive, likes laser, gave me ideas.

## License

Copyright © 2012 Anthony Grimes

Distributed under the Eclipse Public License, the same as Clojure.
