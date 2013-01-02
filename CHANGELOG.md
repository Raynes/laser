## 0.1.12

* Wrote a `node` function to simplify creation of hickory nodes, since you can't
  just drop html into a hickory data structure because it'll be escaped in
  to-html.
* Renamed `select-and` and `select-or` to `and` and `or` because we already
  shadow the `clojure.core/remove` function and you're going to want to
  `require` laser with a qualification anyways.
* `descendant-of`, `adjacent-to`, and `child-of` can all take arbitrary
  arguments now, and are thus much more flexible.
* Added a `zip` function for getting a zipper or seq of zippers from a node or
  seq of nodes.
* Added a `zipper?` function that looks for one of the zipper fns in metadata to
  check of an object is a zipper. Probably not that useful to users, but public
  anyways.
* Added a `select-locs` function that is the same as `select` but returns
  locations rather than nodes.
* `parse-fragment` and `parse` both no longer call `leftmost-descendant` (the
  function that gets a zipper ready for being passed to `fragment` and
  `document`) on the result. Instead, `fragment` and `document` do this. This is
  for the following changes.
* Changed the `select` and `select-locs` function to do a pre-order walk, since
  there is no particular reason to do a post-order walk in this case. This makes
  it easier to do selects on nodes produced from previous `select`s.
