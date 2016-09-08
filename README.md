# Hadoop-MapReduce-

This file describes the implementation of a Hadoop application that takes as input the 50 Wikipedia web pages dedicated to the US states (we will provide these files for consistency) and outputs:

- How many times each of the words “education”, “politics”, “sports”, and “agriculture” appear in the files.

- Rank the top 3 states for each of these words (i.e., which state pages use each of these words the most).

Instructions for creating 4 node cluster from the AMI specified in AMI.txt

1. Go to the AWS EC2 management console, in the navigation panel go to AMIs, under the public images, search for 'ami-874789ea'

2. Select the above ami and launch 4 instances (8GB RAM, Security Group: all traffic)

3. Lets call one of the instance as namenode and other three as datanode1, datanode2 and datanode3

*NOTE*: As described in AMI.txt, set up a config file in the ~/.ssh directory on your local machine so that we do not have to specify the pem key and host address every time we SSH into a node from our local machine. 

4. Open 4 terminals from your local machine and ssh to each instance:
   
   ssh namenode
   ssh datanode1
   ssh datanode2
   ssh datanode3 

5. Copy your permissions file to the namenode:~/.ssh:
    
   scp pem_key_filename ubuntu@namenode:~/.ssh

6. On namenode, open ~/.ssh/config and set the HostName to corresponding Public DNS of namenode and datanodes and IdentityFile to your permissions file

7. On the NameNode we can create the public fingerprint, found in ~/.ssh/id_rsa.pub, and add it first to the NameNode's authorized_keys

   namenode@ssh-keygen -f ~/.ssh/id_rsa -t rsa -P ""
   namenode@cat ~/.ssh/id_rsa.pub >> ~/.ssh/authorized_keys

8. copy the public fingerprint to each DataNode's ~/.ssh/authorized_keys. This should enable the passwordless SSH capabilities from the NameNode to any DataNode.  

   namenode$ cat ~/.ssh/id_rsa.pub | ssh datanode1 'cat >> ~/.ssh/authorized_keys'
   namenode$ cat ~/.ssh/id_rsa.pub | ssh datanode2 'cat >> ~/.ssh/authorized_keys'
   namenode$ cat ~/.ssh/id_rsa.pub | ssh datanode3 'cat >> ~/.ssh/authorized_keys'

9. Common Hadoop Configurations on all Nodes

   As specified in AMI.txt, replace namenode_public_dns with Actual Public DNS of namenode in the following files:
   $HADOOP_CONF_DIR/core-site.xml
   $HADOOP_CONF_DIR/yarn-site.xml
   $HADOOP_CONF_DIR/mapred-site.xml 

10. NameNode Specific Configurations-1

    Open /etc/hosts and add the following lines under localhost (replace with actual public dns and hostnames):

    namenode_public_dns namenode_hostname
    datanode1_public_dns datanode1_hostname
    datanode2_public_dns datanode2_hostname
    datanode3_public_dns datanode3_hostname

11. NameNode Specific Configurations-2

    Create the following directory:

    sudo mkdir -p $HADOOP_HOME/hadoop_data/hdfs/namenode

    Open $HADOOP_CONF_DIR/hdfs-site.xml, Declare the replication factor:

    <configuration>
      <property>
        <name>dfs.replication</name>
        <value>3</value>
      </property>
      <property>
        <name>dfs.namenode.name.dir</name>
        <value>file:///usr/local/hadoop/hadoop_data/hdfs/namenode</value>
      </property>
    </configuration>


12. NameNode Specific Configurations-3

    Create the following file:

    sudo touch $HADOOP_CONF_DIR/masters
 
    Insert namenode's hostname into this file

    $HADOOP_CONF_DIR/masters:

    namenode_hostname

    Modify $HADOOP_CONF_DIR/slaves as follows 

    $HADOOP_CONF_DIR/slaves:

    datanode1_hostname
    datanode2_hostname
    datanode3_hostname

    Change the ownership of the $HADOOP_HOME directory to the user ubuntu:

    sudo chown -R ubuntu $HADOOP_HOME

13. Datanodes Specific Configurations-1

    On each datanode, create the following directory:
    
    sudo mkdir -p $HADOOP_HOME/hadoop_data/hdfs/datanode

    Open $HADOOP_CONF_DIR/hdfs-site.xml, Declare the replication factor:
  
    <configuration>
      <property>
        <name>dfs.replication</name>
        <value>3</value>
      </property>
      <property>
        <name>dfs.datanode.data.dir</name>
        <value>file:///usr/local/hadoop/hadoop_data/hdfs/datanode</value>
      </property>
    </configuration> 

14. Datanodes Specific Configurations-2
  
    Change the ownership of the $HADOOP_HOME directory to the ubuntu user:

    sudo chown -R ubuntu $HADOOP_HOME

15. Start Hadoop Cluster

    namenode$ hdfs namenode -format
    namenode$ $HADOOP_HOME/sbin/start-dfs.sh
    namenode$ $HADOOP_HOME/sbin/start-yarn.sh
    namenode$ $HADOOP_HOME/sbin/mr-jobhistory-daemon.sh start historyserver

                                                                    
INSTRUCTIONS TO RUN THE APPLICATION PROGRAM

16. Create directory /home/ubuntu/inputdata on the namenode and copy all input files into inputdata

17. Create a directory /user in HDFS:

    cd /usr/local/hadoop
    bin/hdfs dfs -mkdir /user

18. Copy the inputdata folder to HDFS:

    cd /usr/local/hadoop/
    bin/hdfs dfs -put '/home/ubuntu/inputdata' /user/

19. Create directory /home/ubuntu/wordcountf on namenode and copy the java application program, WordCount.java into wordcountf

20. Compile the java application program:

    cd /home/ubuntu/wordcountf
    javac -classpath $HADOOP_HOME/share/hadoop/common/hadoop-common-2.7.2.jar:$HADOOP_HOME/share/hadoop/mapreduce/hadoop-mapreduce-client-core-2.7.2.jar:$HADOOP_HOME/share/hadoop/common/lib/commons-cli-1.2.jar -d ./ *.java

21. Create a folder wordcountc inside wordcountf and move all class files into wordcountc

22. Create jar file by executing the command below:
   
    cd /home/ubuntu/wordcountf
    jar -cvf wordcountj.jar -C wordcountc . 

    This will create the jar file named wordcountj.jar

23. Run the application program

    cd /usr/local/hadoop
    bin/hadoop jar ~/wordcountf/wordcountj.jar WordCount /user/inputdata/ output2

24. Output is genetated inside two folders: output1(count of keywords in each state) and output2(top 3 states for each keyword)

    cd /usr/local/hadoop
    hdfs dfs -cat /user/ubuntu/output1/part-r-00000 
    hdfs dfs -cat /user/ubuntu/output2/part-r-00000 

====================================================
