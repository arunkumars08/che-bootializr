FROM fabric8/s2i-java

USER root

RUN yum -y -q install git &&\
        yum clean all &&\
        git config --system user.name jboss &&\
        git config --system user.email jboss@localhost

USER 1000

CMD [ "/usr/local/s2i/run" ]
