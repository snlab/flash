# Flash Artifact for SIGCOMM22 <!-- omit in toc -->

This repo contains the Flash artifact for evaluations in SIGCOMM22 paper "Flash: Fast, Consistent Data Plane Verification for Large-Scale Network Settings".

**Table of Contents**

# Environment setup
## Platform requirements
To run the artifact and run evaluations, the following prefered platform and software suits are required:
* Server hardware requirements: A server with 8+ CPU cores and 32GB+ memory is prefered
* Operating system: Ubuntu Server 20.04 (Other platforms are untested, but should also work as long as bellow software suits are avaliable)
* JDK v1.8.0 or higher version
* Maven v3.8.5 or higher version
* Git v2.25.1 or higher version
* Python v3.6 or higher with pip

***Note***:

Make sure `java` and `mvn` is added to your $PATH environment variable, so that the Flash build script can find them.
  
## Get Flash and datasets for evaluation
Flash artifact is publicly avaliable, clone the repo to any directory to get all required sources for evaluation.
```
$ git clone https://github.com/snlab/flash-public.git
```

## Build Flash and extract datasets
To ease the evaluation process, we provide a build script to build Flash and prepare the datasets for evaluation. Switch to the `flash-public` directory and execute:
```bash
$ cd flash-public
$ git checkout sigcomm22
$ ./build.sh sigcomm22-eval
```
You will see a folder `sigcomm22-eval` is created containing a JAR file(`flash.jar`) and a folder `datasets` containing all extracted datasets. 

Also, an executable script `flash` is generated for easy to use the JAR file. For example:
```
$ cd sigcomm22-eval
$ ./flash -h
```
***Note***:

In the following content, the execution directory is `sigcomm22-eval` if not explicitely specified.

## Command line options
Flash provides a set of command line options as listed bellow. You can also use the following command to inspect all avaliable options.
```bash
$ java -jar flash.jar -h
```

> TBD: A table for cmd options.

# Model Construction Evaluations
> TBD

# Micro Benchmarks
> TBD

# Consistent Efficient Early Detection (CE2D) Evaluations
## Consistent loop detection on OpenR dataset
### Dataset description
To ease the evaluation process, we provide the snapshot of FIB update trace from OpenR. The topology is the same as Internet2.
Each line in `datasets/I2OpenR/trace.txt` uses the follwing format:
```
switchName epochID timestamp prefix mask egress
```
Which is read as "the verifier receives a FIB update from a switch {switchName} at {timestamp}, the FIB update is for IP {prefix}/{mask} going to port {egress} and tagged with {epochID}".

### Run the evaluation
Execute the following command to run the evaluation:
```bash
$ ./flash -d I2OpenR -e
```
### Expected output
Figure 7(a)

## Consistent loop detection on buggy OpenR* dataset
### Dataset description

### Run the evaluation
Execute the following command to run the evaluation:
```bash
$ ./flash -d I2OpenR -e loop
```
### Expected output
Figure 7(b)

## Consistent Loop Detection on I2* dataset
### Dataset description

### Run the evaluation
Execute the following command to run the evaluation:
```bash
$ ./flash -d I2Star -e loop
```
### Expected output
Figure 7(c)

## All pair reachability check on LNet dataset
### Run the evaluation
Execute the following command to run the evaluation:
```bash
$ ./flash -d LNet -e allpair
```
### Expected output
Figure 8