# Introduction

Good evening sirs, madams, and small animals. My name is Raynes and I'd like to
introduce you to this thing I've got here that might help you with templating in
your web apps as well as general HTML transformation. It's called Laser and it
is full of all sorts of good things to help you get your job done, whatever it may
be. Take a seat and I'll explain myself.

## What is Laser?

If you're here, you're probably looking for a HTML templating solution that
won't remove the hair from your kittens (unless, of course, they're supposed to
be hairless). You might have found it.

There are a lot of templating solutions for Clojure and other languages. They
usually work in one of three ways:

* You write HTML in some DSL sort of thing, or Clojure data structures as is the
  case with [hiccup](https://github.com/weavejester/hiccup).
* You write HTML in HTML, but put code directly in the HTML under some strange
  markup as is the case with something like
  [jinja](http://jinja.pocoo.org/). This mixes logic and presentation, something
  some people firmly despise.
* You write HTML with markup that your code will later replace as is the case
  with [mustache](http://mustache.github.com/)

Some people are very happy with any one of the above solutions. I myself have
used Mustache via the excellent
[stencil](https://github.com/davidsantiago/stencil) library and have been very
satisfied. However, these are not the only ways to solve the templating problem.

[Enlive](https://github.com/cgrand/enlive) showed us that we can do elegant
templating with plain HTML and regular code. Templates in Enlive are just plain
HTML. No special markup. Our code takes the HTML in and transforms it using
selectors, similar to how CSS works. Laser works the same way.

Laser is a new alternative to Enlive. It was designed and written because I
wanted something that was as powerful as Enlive, but simpler and more
composable. Enlive uses tagsoup, Laser uses jsoup (via
[hickory](https://github.com/davidsantiago/hickory)). Enlive uses faux css-style
selectors, laser uses plain functions and powerful combinators for
selectors. Laser is also less than half the size of Enlive at the moment. At the
time of laser's creation (this may not always be true), Enlive was mostly
unmaintained and had numerous open pull requests. The code base was too complex
for me to want to pick it up myself, so I create laser as a direct alternative
and will continue to maintain it for the foreseeable future even if Enlive picks
back up.

Laser can be used for templating, any sort of HTML transformations, and for
screen scraping. Its uses and purposes will be explained throughout this guide.

## Starting

First of all, you're going to want to get a new project and add laser to it if
you want to play around.

```
lein new <project>
```

And you'll want to look at [clojars](https://clojars.org/me.raynes/laser) to see
what the latest version is and add it to your `project.clj` file. After that, go
ahead and start up a repl: `lein repl` and follow along.

I bet you'd like to actually see some laser, wouldn't you? Well, here is a
simple example. Say you have this HTML:

```html
<html>
  <head>
  </head>
  <body>
    <p></p>
  </body>
</html>
```

Unfortunately, our paragraph contains no text. Our users are going to abandon us
immediately for the nearest accessible text-filled paragraphed websites! Let's
write some laser to fill in some content!

```clojure
user> (require '[me.raynes.laser :as l])
nil
user> (require '[clojure.java.io :refer [file]])
nil
user> (l/document (l/parse (file "foo.html")) (l/element= :p) (l/content "Hi, I'm a paragraph"))
"<html><head>\n  </head>\n  <body>\n    <p>Hi, I'm a paragraph</p>\n  \n\n</body></html>"
user> (println *1)
<html><head>
  </head>
  <body>
    <p>Hi, I'm a paragraph</p>
  

</body></html>
nil
```

Note that unfortunately, as demonstrated above, the formatting of HTML that goes
in is not entirely preserved. This is mostly out of my control. The idea, of
course, is to maintain equivalence and not exact formatting.

Anyways, look at that! We filled in our paragraph! This example is very
simple. We simply parse the HTML in our file and give it to the `document`
function along with one selector function produced by `element=` and a
transformer function produced by `content`. I will explain how those things work
in the next two sections.

## Selectors

### Nodes

## Transformers

### Seqs of nodes

## Documents and fragments

## Screen scraping

## Getting help
