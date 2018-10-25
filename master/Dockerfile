FROM gettyimages/spark:2.3.1-hadoop-3.0

# FROM continuumio/miniconda3

 ENV LANG=C.UTF-8 LC_ALL=C.UTF-8
 ENV PATH /opt/conda/bin:$PATH

 RUN apt-get update --fix-missing && \
    apt-get install -y wget bzip2 ca-certificates curl git && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

 RUN wget --quiet https://repo.anaconda.com/miniconda/Miniconda3-4.5.11-Linux-x86_64.sh -O ~/miniconda.sh && \
    /bin/bash ~/miniconda.sh -b -p /opt/conda && \
    rm ~/miniconda.sh && \
    /opt/conda/bin/conda clean -tipsy && \
    ln -s /opt/conda/etc/profile.d/conda.sh /etc/profile.d/conda.sh && \
    echo ". /opt/conda/etc/profile.d/conda.sh" >> ~/.bashrc && \
    echo "conda activate base" >> ~/.bashrc

 ENV TINI_VERSION v0.16.1
 ADD https://github.com/krallin/tini/releases/download/${TINI_VERSION}/tini /usr/bin/tini
 RUN chmod +x /usr/bin/tini

 ENTRYPOINT [ "/usr/bin/tini", "--" ]

# MAVEN 

 ENV MAVEN_VERSION 3.5.4
 RUN mkdir /opt/apache-maven
 RUN wget --quiet -c https://apache.rediris.es/maven/maven-3/${MAVEN_VERSION}/binaries/apache-maven-${MAVEN_VERSION}-bin.tar.gz -O apache-maven.tar.gz && \
    tar xzvf apache-maven.tar.gz -C /opt/apache-maven --strip-components=1 && \
    rm -f apache-maven.tar.gz && \
    ln -s /opt/apache-maven/bin/mvn /usr/bin/mvn

 ENV MAVEN_HOME /opt/apache-maven
 ENV PATH MAVEN_HOME:$PATH
 
# Jupyter

 RUN conda install jupyter -y --quiet
 RUN conda install nb_conda_kernels

 # Work dir files

 RUN mkdir /usr/spark-2.3.1/work_dir
 WORKDIR /usr/spark-2.3.1/work_dir
 ADD ./work_dir /usr/spark-2.3.1/work_dir

 # Virtual environment

 RUN conda env create -f datascience35.yaml

 # Compiling Scala example with MAVEN

 WORKDIR /usr/spark-2.3.1/work_dir/scala_apps/example
 RUN mvn package
 
 WORKDIR $SPARK_HOME
 CMD ["bin/spark-class", "org.apache.spark.deploy.master.Master"]

