# Byte Pair Encoding in Java

This project implements the **Byte Pair Encoding (BPE)** algorithm in Java, commonly used in natural language processing (NLP) tasks for subword tokenization. BPE helps reduce vocabulary size and handle out-of-vocabulary words by breaking text into the most frequent symbol pairs.

## Features

- Tokenizes input text using Byte Pair Encoding
- Written in pure Java
- Maven project for easy build and dependency management

## Getting Started

### Prerequisites

- Java 11 or higher
- Maven 3.6+

### Build

Clone the repository and build the project using Maven:

```bash
git clone https://github.com/p-dekker/BytePairEncoding.git
cd BytePairEncoding
mvn clean package