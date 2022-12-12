# AI Assignment 1

University assignment 1 on bayesian networks

## Requirements

- openjdk version: 1.8.x

## Get Started

Run the following commands fot the test network:

```bash
cd ai-assignment-1
make clean
make build
make test
```

## Publish

For publish, run following commands:

```bash
cd ai-assignment-1
make publish
```

## Explain of the project

### Inputs

* XML file path for bayesian network
* queries

### Outputs
* probability, additions, multiplies for every query.

### Data

BNetwork: class that present bayesian network \
VariableClass: class of variableClass information, name, values, length\
Query: class that present query as binary data (keys, etc...)

Notes:
* The algorithms using keys (indexes) and not the actual variableClass names and values.
* Parents saved reverses

## Author

- Omer Priel

## License

MIT
