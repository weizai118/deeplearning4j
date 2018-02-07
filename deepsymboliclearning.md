---
title: About the Deeplearning4j Team
layout: default
---

# Deep Symbolic Learning: Hybrid Methods to Combine Deep Learning with Symbolic Learning

The two biggest knocks on deep learning are lack of model interpretability (why did my model make that prediction?) and the amount of data deep neural networks require to learn. Geoff Hinton himself has [expressed scepticism](https://www.axios.com/artificial-intelligence-pioneer-says-we-need-to-start-over-1513305524-f619efbd-9db0-4947-a9b2-7a4c310a28fe.html) about whether backpropagation, the workhorse of deep neural nets, will be the way forward for AI.

Research into so-called one-shot learning could address deep learning's data hunger, while deep symbolic learning, or enabling deep neural networks to generate and manipulate concepts, could help solve explainability.<sup>[1](#one)</sup>   

This page includes recent, interesting research that attempts to combine deep learning with symbolic learning. 

## Deep Symbolic Learning Resources

* [Towards Deep Symbolic Reinforcement Learning](https://arxiv.org/abs/1609.05518) (2016)
by Marta Garnelo, Kai Arulkumaran and Murray Shanahan

*Deep reinforcement learning (DRL) brings the power of deep neural networks to bear on the generic task of trial-and-error learning, and its effectiveness has been convincingly demonstrated on tasks such as Atari video games and the game of Go. However, contemporary DRL systems inherit a number of shortcomings from the current generation of deep learning techniques. For example, they require very large datasets to work effectively, entailing that they are slow to learn even when such datasets are available. Moreover, they lack the ability to reason on an abstract level, which makes it difficult to implement high-level cognitive functions such as transfer learning, analogical reasoning, and hypothesis-based reasoning. Finally, their operation is largely opaque to humans, rendering them unsuitable for domains in which verifiability is important. In this paper, we propose an end-to-end reinforcement learning architecture comprising a neural back end and a symbolic front end with the potential to overcome each of these shortcomings. As proof-of-concept, we present a preliminary implementation of the architecture and apply it to several variants of a simple video game. We show that the resulting system -- though just a prototype -- learns effectively, and, by acquiring a set of symbolic rules that are easily comprehensible to humans, dramatically outperforms a conventional, fully neural DRL system on a stochastic variant of the game.*

* [Learning explanatory rules from noisy data](https://deepmind.com/blog/learning-explanatory-rules-noisy-data/) (2018)
by Richard Evans and Edward Grefenstette 

*Artificial Neural Networks are powerful function approximators capable of modelling solutions to a wide variety of problems, both supervised and unsupervised. As their size andexpressivity increases, so too does the variance of the model, yielding a nearly ubiquitous overfitting problem. Although mitigated by a variety of model regularisation methods, the common cure is to seek large amounts of training data—which is not necessarily easily obtained—that sufficiently approximates the data distribution of the domain we wish to test on. In contrast, logic programming methods such as Inductive Logic Programming offer an extremely data-efficient process by which models can be trained to reason on symbolic domains. However, these methods are unable to deal with the variety of domains neural networks can be applied to: they are not robust to noise in or mislabelling of inputs, and perhaps more importantly, cannot be applied to non-symbolic domains where the data is ambiguous, such as operating on raw pixels. In this paper, we propose a Differentiable Inductive Logic framework, which can not only solve tasks which traditional ILP systems are suited for, but shows a robustness to noise and error in the training data which ILP cannot cope with. Furthermore, as it is trained by backpropagation against a likelihood objective, it can be hybridised by connecting it with neural networks over ambiguous data in order to be applied to domains which ILP cannot address, while providing data efficiency and generalisation beyond what neural networks on their own can achieve.*

* [Schema Networks: Zero-shot Transfer with a Generative Causal Model of Intuitive Physics](https://arxiv.org/abs/1706.04317) (2017)

*The recent adaptation of deep neural network-based methods to reinforcement learning and planning domains has yielded remarkable progress on individual tasks. Nonetheless, progress on task-to-task transfer remains limited. In pursuit of efficient and robust generalization, we introduce the Schema Network, an object-oriented generative physics simulator capable of disentangling multiple causes of events and reasoning backward through causes to achieve goals. The richly structured architecture of the Schema Network can learn the dynamics of an environment directly from data. We compare Schema Networks with Asynchronous Advantage Actor-Critic and Progressive Networks on a suite of Breakout variations, reporting results on training efficiency and zero-shot generalization, consistently demonstrating faster, more robust learning and better transfer. We argue that generalizing from limited data and learning causal relationships are essential abilities on the path toward generally intelligent systems.*

* [Deep Learning: A Critical Appraisal](https://arxiv.org/abs/1801.00631) (2018)
by Gary Marcus

*Although deep learning has historical roots going back decades, neither the term "deep learning" nor the approach was popular just over five years ago, when the field was reignited by papers such as Krizhevsky, Sutskever and Hinton's now classic (2012) deep network model of Imagenet. What has the field discovered in the five subsequent years? Against a background of considerable progress in areas such as speech recognition, image recognition, and game playing, and considerable enthusiasm in the popular press, I present ten concerns for deep learning, and suggest that deep learning must be supplemented by other techniques if we are to reach artificial general intelligence.*

### <a name="beginner">Other Deep Learning Tutorials</a>
* [LSTMs and Recurrent Networks](./lstm)
* [Introduction to Deep Neural Networks](./neuralnet-overview)
* [Convolutional Networks](./convolutionalnets)
* [Restricted Boltzmann Machines](./restrictedboltzmannmachine)
* [Eigenvectors, Covariance, PCA and Entropy](./eigenvector)
* [Deep Reinforcement Learning](.deepreinforcementlearning)
* [Deeplearning4j Quickstart Examples](./quickstart)
* [ND4J: A Tensor Library for the JVM](http://nd4j.org)
* [MNIST for Beginners](./mnist-for-beginners.html)
* [Glossary of Deep-Learning and Neural-Net Terms](./glossary.html)
* [Generative Adversarial Networks (GANs)](./generative-adversarial-network)
* [Word2vec and Natural-Language Processing](./word2vec.html)

<a name="one">1)</a> The two problems may overlap, and solving one could lead to solving the other, since a concept that helps explain a model will also help it recognize certain patterns in data using fewer examples. 