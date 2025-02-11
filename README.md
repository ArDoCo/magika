# Magika

This is a Java library that includes [Google's magika](https://github.com/google/magika).

According to the README at [google/magika](https://github.com/google/magika):
> Magika is a novel AI powered file type detection tool that relies on the recent advance of deep learning to provide accurate detection. Under the hood, Magika employs a custom, highly optimized Keras model that only weighs about a few MBs, and enables precise file identification within milliseconds, even when running on a single CPU.

> In an evaluation with over 1M files and over 100 content types (covering both binary and textual file formats), Magika achieves 99%+ precision and recall. Magika is used at scale to help improve Google users’ safety by routing Gmail, Drive, and Safe Browsing files to the proper security and content policy scanners. Read more in our [research paper](https://arxiv.org/abs/2409.13768!

> The Magika paper was accepted at IEEE/ACM International Conference on Software Engineering (ICSE) 2025!

See also [their website](https://google.github.io/magika/).

## Getting Started

Coming soon!


## Known Limitations & Contributing

Magika significantly improves over the state of the art, but there's always room for improvement! More work can be done to increase detection accuracy, support for additional content types, bindings for more languages, etc.

This initial release is not targeting polyglot detection, and we're looking forward to seeing adversarial examples from the community. We would also love to hear from the community about encountered problems, misdetections, features requests, need for support for additional content types, etc.


## License
Apache 2.0; see [LICENSE](LICENSE) for details.

The model is licensed by the original authors under Apache 2.0, see their [LICENSE](./src/main/resources/magika/LICENSE).