# Word Verifier

## Background
This is an experiment with two datastructures 
[Tries](https://en.wikipedia.org/wiki/Trie) and
[BK Trees](https://en.wikipedia.org/wiki/BK-tree).

This is a Java Spring application that takes a word list and adds it to
both a trie and BK Tree.  Then, using a call to a REST Controller, a client passes
in a number of words.   Those words are looked up to see if they're in the Trie or not.
If the word is not in the trie, it will look in the BK-Tree to determine what of the words that
_are_ there have a Levenstein distance of two.   This will return
words that are close to the mis-spelled word.

So, given a REST call like:
```
 curl -X POST http://localhost:8080/verify
     -H "Content-Type: text/plain"
     -d "hello wrold progaming java"
```
the following may be returned.

```
✓ hello
✘ wrold; word, world
✘ progaming; programming
✓ java
```

 



## Maintainers
[David](https://github.com/david-Darkcollective)

## Contributing

## License
[MIT License](LICENSE) - © Dark Collective, LLC
