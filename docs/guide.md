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
[hickory](https://github.com/davidsantiago/hickory ) ). Enlive uses faux css-style
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

The reason you don't have to put any markup in your HTML for laser to work is
because it walks your HTML itself. Because of this, a selector-based approach to
changing things is possible. Unlike Enlive which uses a vector of keywords/faux
css-selector approach, Laser simply uses selectors and combinators. Just
functions. It's pretty simple. First of all, let's look at what a node is.

### Nodes

Nodes are maps that contain data about HTML. The nodes laser uses are hickory
nodes, ones used by the hickory library. It is similar to `clojure.xml`'s
format, but with an extra `:type` key so that they are better suited to HTML
data. Here is what a node looks like:

```clojure
{:type :element
 :tag :a
 :attrs {:href "https://github.com/Raynes/laser"}
 :content ["laser"]}
```

This node corresponds to this HTML:

```html
<a href="laser">laser</a>
```

Nodes are, of course, nested in the `:content` of other nodes to build the HTML
tree.

#### Creating new nodes

You can generate hickory nodes in Clojure with this simple `node` function:

```clojure
user> (l/node :a :attrs {:href "https://github.com/Raynes/laser"} :content "laser")
{:tag :a, :type :element, :content ["laser"], :attrs {:href "https://github.com/Raynes/laser"}}
```

All args except for the first (the tag name) are optional.

### Anatomy of a selector

Selectors are just functions that take a location in the tree, do something with
it, and return truthy or falsey. We will usually write higher order functions
that return selectors. For example, `element=` is that kind of function.
Here is how the `element=` selector is written:

```clojure
user> (defn element= [element] (fn [loc] (= element (-> loc clojure.zip/node :tag))))
#'user/element=
```

Easy enough, right? We defined a function called `element=` that takes an
element that we'd like to match on. We return a function that takes a zipper
location, and we check to see if the element passed in is equal to the tag of
the node at this location in the tree. Please see the section on nodes above for
an explanation of nodes.

Let's test it:

```clojure
user> (require '[hickory.zip :refer [hickory-zip]])
nil
user> (let [selector (l/element= :a)] (selector (hickory-zip {:type :element :tag :a})))
true
user> (let [selector (l/element= :a)] (selector (hickory-zip {:type :element :tag :div})))
false
```

And that is literally all there is to it. A selector is just a function that
takes a loc and returns truthy or falsey, but we also refer to functions like
`element=` as 'selectors', since they return the actual `selector` functions.

Selector functions can be pretty complex and flexible. They receive a zipper, so
all the typical zipper functions work with that input and you can move around
the tree to do whatever checking is necessary. For examples of complex selector
combinators that make use of this capability, check out `decendant-of` and
similar functions. In fact, take a look at them anyway! There are selectors for
all sorts of stuff!

### Composing selectors

Since selectors are just functions, we can compose them easily. What if, for
example, we wanted to match an `<a>` tag only if it has the class `foo`? We can
do that!

```
user> ((l/and (l/element= :a) (l/class= "foo")) (hickory-zip {:type :element :tag :a :attrs {:class "bar foo baz"}}))
true
user> ((l/and (l/element= :a) (l/class= "foo")) (hickory-zip {:type :element :tag :a :attrs {:class "bar baz"}}))
false
```

This makes use of one of our selector combinators, `and`. It is just like
Clojure's `and`, but works on selectors. There is also an `or`. There are lots
of these fancy combinators, take a look at the source or
[API docs](http://raynes.github.com/laser) to see them all.

## Transformers

Transformers are even simpler than selectors! They are just functions that take
a node (a map, just like we showed in the node section above) and return a node,
a seq of nodes, a string, or nil. Here is a simple transformer:

```
user> (defn attr [attr value] (fn [node] (assoc-in node [:attrs attr] value)))
#'user/attr
```

The above function takes an attribute (like `:href`) and a value to set that
attribute to. It returns a function that takes a node and sets the attribute to
the value we specified. Simple enough, right? Same concept as with selectors.

```clojure
user> ((l/attr :href "https://github.com/Raynes/laser") {:type :element :tag :a})
{:attrs {:href "https://github.com/Raynes/laser"}, :type :element, :tag :a}
```

Excellent!

### Escaping

If you're constructing strings based on user input and then sticking them in
your HTML, you definitely want to make sure they are escaped. Good news is that
laser *always* escapes any strings you give it by default. In order to keep it
from escaping a string you have to call the `me.raynes.laser/unescaped` function
on the string. This mechanism was implemented to give people an escape hatch
that didn't involve parsing their HTML if they already had it stored (like
in a database), but also required thinking things through first. `unescaped`
simply wraps the string in a `RawHTML` type that tells hickory to not escape it.

**One gotcha here: if your node is of type `:script`, you should not use
`unescaped` for it's `:content`.** So, if you are seeing `hickory.core.RawHTML` in your
generated HTML, then it's probably because you're trying to escape the `:content`
of a `:script` node.

### Seqs of nodes

The case of returning a single node is pretty simplistic. What if we
wanted to generate a bunch of nodes based on an `<a>` tag inside of a div? We
can do that easily with a transformer. First of all, let's get our HTML tree
(we're purposely avoiding using HTML parsing functions in laser because that
section comes later):

```clojure
user> (def html (l/node :div :content (l/node :a :attrs {:class "link"})))
#'user/html
user> html
{:tag :div, :type :element, :content [{:tag :a, :type :element, :content nil, :attrs {:class "link"}}], :attrs nil}
```

Now we have a usable hickory HTML representation. Next, let's get some links:

```clojure
user> (def links ["https://github.com/Raynes/laser"
                  "https://github.com/Raynes/conch" 
                  "https://github.com/Raynes/refheap"])
#'user/links
```

```clojure
user> (l/to-html
        (l/at html 
              (l/element= :a) #(for [link links] 
                                 (-> % 
                                     (assoc-in [:attrs :href] link)
                                     (assoc :content [link])))))

"<div><a href=\"https://github.com/Raynes/laser\" class=\"link\">https://github.com/Raynes/laser</a><a href=\"https://github.com/Raynes/conch\" class=\"link\">https://github.com/Raynes/conch</a><a href=\"https://github.com/Raynes/refheap\" class=\"link\">https://github.com/Raynes/refheap</a></div>"
```

This is our longest example yet! it's actually fairly simple though. We've
introduced something completely new here, the `at` function. It will be
explained later, but a simple explanation is that it takes a node and some
selector and transformer pairs and walks the tree applying the transformations
when the selectors match. In this case, we're matching on `:a` elements, and
there is exactly one. Our transformer is a function that (of course) takes a
node and returns a seq of nodes to replace that node with. We're using `for` to
iterate through our `links` vector and produce a new `<a>` node for each
link. We're setting `:href` as well as the link's content. Finally, we call
`to-html` on the result to get HTML back. This is likely one of the most
complicated transformers you'll see.

#### Behavior difference from Enlive

**Don't bother reading this section unless you know why you're here. It's more
of a history lession than anything, and might answer questions if you've noticed
laser behaving differently from Enlive.**

Laser behaves a bit differently than Enlive does when a transformer returns a
seq of nodes. Let's take a look at a scenario to demonstrate the difference.

You have two selectors, x and y. Both of them match any given `<a>` tag. Your
x selector, which goes first, hits an `<a>` tag. Its transformer is called on
this node and it returns a seq of 4 new `<a>` tags. But you still have to call
the y selector. What do you call it on? Selectors are meant to run on a single
location in a zipper, but now you have 4 completely new nodes. Here is how
Enlive and laser both handle these cases.

Enlive handles this scenario by running the y selector on all of those new tags
I'm not entirely certain how it goes about this. Laser handles it by running the
y selector on the last tag in the seq. It does this by simply inserting each new
node into the zipper and then continuing with that location and the following
selector.

My rationale for doing it this way is as follows: it is simple and I'm not
convinced that we ever need the enlive-like behavior. What I'm doing now is very
simple, whereas doing what Enlive does is much more non-trivial, though it may
seem like it wouldn't be. You can't just run the new selectors on the returned
nodes. They have to be inserted into the zipper so that selectors like
`descendant-of` can dig into the tree at higher levels than that node. Could we
do it like Enlive does it? Yes, but it'd make the code quite a bit more
complicated and I haven't seen a case where someone relied on Enlive's
behavior. If you do, let me know about it and explain why you need it and I'll
work on it.

### Composing transformers

Transformers are just functions, so they can be easily composed! As a matter of
fact, we usually don't even need any special functions for them! For example,
what if you wanted to chain two or more transformers together? Easy:

```clojure
user> ((comp (l/classes "foo") (l/id "bar")) {:type :element :tag :a})
{:attrs {:class "foo", :id "bar"}, :type :element, :tag :a}
```

Yes, that's just plain old `comp`. Nothing special here.

## Documents and fragments

Now that we've learned about selectors and transformers, we need to learn to tie
them into structured transformations on HTML. Reusable things. Let's take it
step by step.

### Parsing HTML

If you're working with HTML, you're going to have to parse it. Luckily, we can
do that. We wouldn't be very useful if we couldn't.

Hickory handles the parsing, in turn delegating it to JSoup, a strict validating
HTML 5 parser. Laser provides convenience functions for producing things
suitable for passing to laser functions like `fragment` and `document`.

We can parse a full HTML *document* using the `parse` function.

```clojure
user> (l/parse "<a></a>")
[{:type :document, :content [{:type :element, :attrs nil, :tag :html, :content [{:type :element, :attrs nil, :tag :head, :content nil} {:type :element, :attrs nil, :tag :body, :content [{:type :element, :attrs nil, :tag :a, :content nil}]}]}]} nil]
```

We get back a zipper over the hickory HTML representation that we can pass to
`document` (explained shortly). Notice how the `<html>`, `<head>`, and `<body>`
tags were all added? This is because a document always produces a fully HTML 5
compliant HTML document. If we need to parse only a fragment of HTML, we need to
use `fragment` (duh)!

```clojure
user> (l/parse-fragment "<a></a><p></p>")
([{:type :element, :attrs nil, :tag :a, :content nil} nil] [{:type :element, :attrs nil, :tag :p, :content nil} nil])
```

In this example, we get back a seq of zippers over hickory HTML
representations. We've got one for the `<a>` tag and one for the `<p>` tag. Note
that the HTML pieces in a fragment still have to be compliant HTML, just not a
full document. For example, we can't parse random `<td>` nodes without the
actual accompanying `<table>`:

```clojure
user> (l/parse-fragment "<td></td>")
()
user> (l/parse-fragment "<table><td></td></table>")
([{:type :element, :attrs nil, :tag :table, :content [{:type :element, :attrs nil, :tag :tbody, :content [{:type :element, :attrs nil, :tag :tr, :content [{:type :element, :attrs nil, :tag :td, :content nil}]}]}]} nil])
```

The first example would not parse because it isn't valid HTML. The second
example parses, but even then it is corrected to add a `<tbody>` tag. This
behavior may not be what you want in all cases, but I can't really work around
it. This behavior is subject to change (but likely wouldn't negatively effect
anyone) if JSoup ever adds a way to parse a raw fragment without correcting or
forcing HTML compliancy.

Both of the parsing functions support anything usable by Clojure's `slurp`
function as well as simple strings of HTML. Feel free to pass a `File`,
resource, `Reader`, etc.

Now, on to working with documents and fragments.

### Document transformation

Laser is designed to be composable so you can write small structured
transformations. However, eventually you need to tie everything you've done
together. This is what `document` is for. It is meant to be the final
transformation in your chain of transformations, and will return a fully HTML 5
compliant document for you to throw at a web browser. Here is an example:

```clojure
user> (def html (l/parse "<div><p>Welcome to fooland</p><p>May I take your order?</p></div>"))
#'user/html
user> (l/document html (l/element= :p) (l/classes "paragraph"))
"<html><head></head><body><div><p class=\"paragraph\">Welcome to fooland</p><p class=\"paragraph\">May I take your order?</p></div></body></html>"
```

That's all there is to `document` transformations. They simply take some parsed HTML and some
selector and transformer pairs, and then they iterate through the HTML tree
replacing nodes that match the selectors with whatever the transformers return
on them! They are more interesting when used with fragments.

### Fragment transformation

A `fragment` transformation is a transformation applied to a fragment of HTML
from `parse-fragment` or a seq of manually created nodes. While `document`
returns a string of HTML, `fragment` returns a seq of nodes. The reason for this
is because fragments are typically the building blocks to a document, and you'll
likely end up passing the resulting nodes to a `document` call, or another
`fragment`. Here is a trivial example:

```clojure
user> (def frag (l/parse-fragment "<p>Welcome to fooland</p><p>May I take your order?</p>"))
#'user/frag
user> (def docu (l/parse "<div></div>"))
#'user/docu
user> (defn paragraph-with-class [class] (l/fragment frag (l/element= :p) (l/classes class)))
#'user/paragraph-with-class
user> (l/document docu (l/element= :div) (l/content (paragraph-with-class "paragraph")))
"<html><head></head><body><div><p class=\"paragraph\">Welcome to fooland</p><p class=\"paragraph\">May I take your order?</p></div></body></html>"
```

In this example, we define `frag` as parsed `<p>` tags, and `docu` as the
(albeit extremely contrived example of a) top-level document. We do the same
thing as before, but this time we make use of `fragment`. Our original example
is obviously preferrable, but in larger HTML transformations, you certainly want
to make use of `fragment` to simplify and split your code into smaller,
composable pieces.

### Conditionals and other fun things

You may find it somewhat restricting that you have to give `fragment` and
`document` flat selector and transformer pairs. What if, for example, you want
to only have one pair if some argument isn't nil? Well, you can actually give
`fragment` and `document` seqs of selectors and transformers nested as deeply as
you need. This means you can do stuff like this:

```clojure
(defn foo [x]
  (l/fragment some-html
             (l/element= :foo) (l/content "bar")
             (when x
               [(l/element= x) (l/content "baz")])))
```

Since `fragment` (and `document`) flattens the the resulting functions and
filters out things that aren't functions, this will work. If the `when` returns
nil then it'll be filtered out, but if it returns the selector and transformer
pair, it'll be used.

### defdocument and defragment

There are two convenience macros for defining document and fragment
transformation functions. They are `defdocument` and `defragment`. They both
have the same features that make them useful:

* Instead of taking pre-parsed HTML, they take anything that is readable by
  Clojure's `slurp` function. This means a file, URL, or more importantly a
  resource of a file on the classpath (as per `clojure.java.io/resource`).
* Parsing is only done once as soon as the function is defined. It would be
  extremely wasteful to parse the HTML every time the function is called.

Here is one of our earlier examples:

```clojure
user> (l/document (l/parse (file "foo.html")) (l/element= :p) (l/content "Hi, I'm a paragraph"))
```

Let's rewrite this with `defdocument`:

```clojure
user> (l/defdocument foo (file "foo.html") [content] (l/element= :p) (l/content content))
#'user/foo
user> (foo "Hi, I'm a paragraph")
"<html><head>\n  </head>\n  <body>\n    <p>Hi, I'm a paragraph</p>\n  \n\n</body></html>"
```

I took the liberty of removing the hardcoded content and making it an
argument. Now you can pass whatever content you like! Unfortunately I am not
very creative, so I ended up passing the same old thing anyways. Maybe you can
think of something more trendy.

So, `defdocument` is simple enough. There is one gotcha though: the body of the
function can only contain selector and transformer pairs. You can't, for
example, wrap the body with `let`. To help mitigate that a bit, you can
optionally pass a bindings vector after the argument vector, and it'll be added
to a `let` that wraps the body.

```clojure
user> (l/defdocument foo (file "foo.html") [content] [content (str content "...")] (l/element= :p) (l/content content))
#'user/foo
user> (foo "Hi, I'm a paragraph")
"<html><head>\n  </head>\n  <body>\n    <p>Hi, I'm a paragraph...</p>\n  \n\n</body></html>"
```

This covers the most useful case, I think, but of course there are others. What
if you wanted to wrap everything in `bindings`, for example. These macros are
fairly small and simple, and they are currently the only macros in laser. It is
easy enough to use `fragment` and `document` directly, so rather than create
huge ugly macros to do what we should be doing with functions, I'll leave things
the way they are. Only use `defdocument` and `defragment` in cases where they
fit the problem you're trying to solve purposely. Don't try to make them work
where they aren't meant to work. They are just convenience macros for a common
usage pattern.

I'm not going to bother talking about `defragment`, since it is precisely the
same as `defdocument` but instead parses with `parse-fragment` and uses the
`fragment` function under the hood, just like you'd expect!

### at

Sometimes you might find yourself with a hickory node that you'd like to run
some selectors and tranformers on. It's easy:

```clojure
user> (l/at (l/node :div :content (l/node :p :content "Hi")) (l/element= :p) #(repeat 3 %))
{:tag :div, :type :element, :content [{:tag :p, :type :element, :content ["Hi"], :attrs nil} {:tag :p, :type :element, :content ["Hi"], :attrs nil} {:tag :p, :type :element, :content ["Hi"], :attrs nil}], :attrs nil}
```

This is probably pretty self explanatory by now, but in case it isn't: we've
passed it a hickory representation of HTML (a node), a selector that matches
`<p>` tags, and a transformer that duplicates whatever tag it gets 3 times. The
end result is that our old `<p>` tag is replaced with 3 more just like it.

## Screen scraping

Laser's selector approach lends itself well to screen scraping. We have two
functions for that, `select` and `select-locs`.

For a moment, let's assume we have dire need of all of the full paste links on
[Refheap's latest pastes page](https://www.refheap.com/pastes). Take a look at
the source of that HTML. Here is how we can extract all of the paste links:

```clojure
user> (def html (l/parse (slurp url)))
#'user/html
user> (l/select html (l/child-of (l/class= "preview-header") (l/element= :a)))
({:type :element, :attrs {:href "/paste/8206"}, :tag :a, :content ["Paste 8206"]} {:type :element, :attrs {:href "/paste/8205"}, :tag :a, :content ["Paste 8205"]} {:type :element, :attrs {:href "/paste/8204"}, :tag :a, :content ["Paste 8204"]} {:type :element, :attrs {:href "/paste/8203"}, :tag :a, :content ["Paste 8203"]} {:type :element, :attrs {:href "/paste/8202"}, :tag :a, :content ["Paste 8202"]} {:type :element, :attrs {:href "/paste/8201"}, :tag :a, :content ["Paste 8201"]} {:type :element, :attrs {:href "/paste/8200"}, :tag :a, :content ["Paste 8200"]} {:type :element, :attrs {:href "/paste/8199"}, :tag :a, :content ["Paste 8199"]} {:type :element, :attrs {:href "/paste/8198"}, :tag :a, :content ["Paste 8198"]} {:type :element, :attrs {:href "/paste/8197"}, :tag :a, :content ["Paste 8197"]} {:type :element, :attrs {:href "/paste/8194"}, :tag :a, :content ["Paste 8194"]} {:type :element, :attrs {:href "/paste/8193"}, :tag :a, :content ["Paste 8193"]} {:type :element, :attrs {:href "/paste/8192"}, :tag :a, :content ["Paste 8192"]} {:type :element, :attrs {:href "/paste/8191"}, :tag :a, :content ["Paste 8191"]} {:type :element, :attrs {:href "/paste/8189"}, :tag :a, :content ["Paste 8189"]} {:type :element, :attrs {:href "/paste/8188"}, :tag :a, :content ["Paste 8188"]} {:type :element, :attrs {:href "/paste/8186"}, :tag :a, :content ["Paste 8186"]} {:type :element, :attrs {:href "/paste/8185"}, :tag :a, :content ["Paste 8185"]} {:type :element, :attrs {:href "/paste/8184"}, :tag :a, :content ["Paste 8184"]} {:type :element, :attrs {:href "/paste/8182"}, :tag :a, :content ["Paste 8182"]})
user> (for [{{href :href} :attrs} *1] (str "https://www.refheap.com" href))
("https://www.refheap.com/paste/8206" "https://www.refheap.com/paste/8205" "https://www.refheap.com/paste/8204" "https://www.refheap.com/paste/8203" "https://www.refheap.com/paste/8202" "https://www.refheap.com/paste/8201" "https://www.refheap.com/paste/8200" "https://www.refheap.com/paste/8199" "https://www.refheap.com/paste/8198" "https://www.refheap.com/paste/8197" "https://www.refheap.com/paste/8194" "https://www.refheap.com/paste/8193" "https://www.refheap.com/paste/8192" "https://www.refheap.com/paste/8191" "https://www.refheap.com/paste/8189" "https://www.refheap.com/paste/8188" "https://www.refheap.com/paste/8186" "https://www.refheap.com/paste/8185" "https://www.refheap.com/paste/8184" "https://www.refheap.com/paste/8182")
```

Nice, huh?

`select-locs` isn't worth talking about, because it is precisely the same as
`select`, but returns zipper locations rather than nodes. You'll usually want
`select`, and you'll certainly realize when you need `select-locs`.

# Goodbye and how to get help

Well, this concludes our professional's guide to operating a powerful laser beam
device. I hope it helped you and that laser makes all your dreams come true. On
the off chance that it doesn't and you have a problem here is what you should
do:

* Do you think your problem is a bug? Open an issue here on Github!
* Do you have a feature request? Open an issue here on Github!
* Do you need help writing some laser code, have broken code that you can't
  figure out how to fix, or want some advice on how to solve a particular
  problem with laser? Feel free to contact me directly via email (it's on my
  Github profile), or preferrably on the #clojure IRC channel on the freenode
  IRC network. I am always in that channel, and if I don't respond for several
  hours I'm probably asleep. Make no assumptions as to my sleeping
  behaviors. The good thing about IRC is that even if I'm not there, someone
  else who is familiar with laser may be, and they might be able to help you. Ya
  just gotta ask!

Now go and etch your name into the internet with laser!
