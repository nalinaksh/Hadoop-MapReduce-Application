/*
Filename:    WordCount.java
Author:      Nalinaksh Gaur
UCID:        ng294
Description: Two MapReduce jobs are serialized. The input data is a collection of files, one for each state.
             The output of 1st MR job produces a file named 'output1' containing the count of occurance of 
             keywords (agriculture, education, politics, sports) in each file. Output is in the form: 
             "state:keyword count".
             The input for 2nd MR job is the file 'output1'. The output of 2nd MR job is the top 3 states 
             for each of the keywords (agriculture, education, politics, sports). Output is in the 
             form: "keyword:count state".
*/

import java.io.IOException;
import java.util.StringTokenizer;
import java.io.*;
import java.util.*;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.input.FileSplit;
import org.apache.hadoop.io.WritableComparable;
import org.apache.hadoop.io.WritableComparator;
import org.apache.hadoop.io.WritableUtils;
import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.mapreduce.Partitioner;

public class WordCount {

    //Strucutre of the Composite key(state:keyword) outputted from 1st Mapper
    //Start class CompositeGroupKey
    public static class CompositeGroupKey implements
        WritableComparable<CompositeGroupKey> {
        //state is derived from input filenames
        Text state;
        //keyword ('agriculture', 'education', 'polotics', or 'sports')
        Text keyword;

        public CompositeGroupKey(Text state, Text keyword) {
            this.state = state;
            this.keyword = keyword;
        }

        public CompositeGroupKey() {
            this.state = new Text();
            this.keyword = new Text();
        }

        public void setCompositeKey(Text state, Text keyword)
        {
            this.state = state;
            this.keyword = keyword;
        }

        public void write(DataOutput out) throws IOException {
            this.state.write(out);
            this.keyword.write(out);
        }

        public void readFields(DataInput in) throws IOException {
            this.state.readFields(in);
            this.keyword.readFields(in);
        }

        public int compareTo(CompositeGroupKey pop) {
            if (pop == null)
              return 0;
            int intcnt = state.compareTo(pop.state);
              return intcnt == 0 ? keyword.compareTo(pop.keyword) : intcnt;
        }

        @Override
        public String toString() {
          return state.toString() + ":" + keyword.toString();
        }
      
        public Text getState() {
          return state;
        }
      
        public Text getKeyword() {
          return keyword;
        }
    }
    //End class CompositeGroupKey
    
    //1st Mapper: 
    //Input  : state filename
    //Output : composite key(state:keyword), value(1)
    //Start class TokenizerMapper
    public static class TokenizerMapper
       extends Mapper<Object, Text, CompositeGroupKey, IntWritable>{

    private Text word = new Text();
    private Text filename = new Text(); 
    private String str;
    private final static IntWritable one = new IntWritable(1);
    //delimiter string used to word tokenize the state files 
    private String delim = new String(" .:;-_,+#&<>[]()/\\\"");
    
    public void map(Object key, Text value, Context context
                    ) throws IOException, InterruptedException {
    
      //word tokenize the state input file based on the delimiter string
      StringTokenizer itr = new StringTokenizer(value.toString(), delim);
      
      //get the input filename (which is the state name for this programming project) 
      String name = ((FileSplit) context.getInputSplit()).getPath().getName();
       
      CompositeGroupKey cgk = new CompositeGroupKey();
      
      //Only emit a key value pair if the word token is any of the four keywords : 'agriculture', 'education', 'politics' or 'sports'
      while (itr.hasMoreTokens()) {
          str = itr.nextToken();
          if (str.toLowerCase().equals("education") ||
            str.toLowerCase().equals("politics") ||
            str.toLowerCase().equals("sports") ||
            str.toLowerCase().equals("agriculture") )
          {
            word.set(str.toLowerCase());
            filename.set(name);
        
            //create a composite key with (state:keyword)
            cgk.setCompositeKey(filename, word);
        
            //emit key value pair, value being 1 for each key
            context.write(cgk, one);
          }
        }
      }
    }
    //End class TokenizerMapper

    //1st Reducer
    //Input : key(state:keyword), value(1)
    //Output: Key(state: keyword), value(count)
    //Start class IntSumReducer
    public static class IntSumReducer
       extends Reducer<CompositeGroupKey,IntWritable,CompositeGroupKey,IntWritable> {
    private IntWritable result = new IntWritable();

      //Count the occurance for each key (state:keyword) emitted from Mapper
      public void reduce(CompositeGroupKey key, Iterable<IntWritable> values,
                       Context context
                       ) throws IOException, InterruptedException {
        int sum = 0;
        for (IntWritable val : values) {
          sum += val.get();
        }
        result.set(sum);
        //write key, value pair to the output file
        context.write(key, result);
      }
    }
    //End class IntSumReducer


    //Structure of composite key (keyword:count) that will be output by 2nd Mapper
    //Start class CompKey 
    public static class CompKey implements WritableComparable<CompKey> {

	    private String keyword;
	    private Long count;
	
	    public CompKey() { }
	
	    public CompKey(String keyword, Long count) {
		  this.keyword = keyword;
		  this.count = count;
	    }
	
	    public void SetCompKey(String keyword, Long count) {
	      this.keyword = keyword;
	      this.count = count;
	    }
	
	    @Override
	    public String toString() {
		  return (new StringBuilder())
				.append(keyword)
				.append(':')
				.append(count)
				.toString();
	    }
	
	    @Override
	    public void readFields(DataInput in) throws IOException {
		  keyword = WritableUtils.readString(in);
		  count = in.readLong();
	    }

	    @Override
	    public void write(DataOutput out) throws IOException {
		  WritableUtils.writeString(out, keyword);
		  out.writeLong(count);
	    }

	    @Override
	    public int compareTo(CompKey o) {
		  int result = keyword.compareTo(o.keyword);
		  if(0 == result) {
			result = count.compareTo(o.count);
		  }
		  return result;
	    }

	    public String getKeyword() {
		  return keyword;
	    }

	    public void setKeyword(String keyword) {
		  this.keyword = keyword;
	    }

	    public Long getCount() {
		  return count;
	    }

	    public void setCount(Long count) {
		  this.count = count;
	    }
    }
    //End class CompKey

    /*
    The 3 classes mentioned below: CompositeKeyComparator, PrimaryKeyGroupingComparator 
    & PrimaryKeyPartitioner will be used to sort the composite keys generated by 2nd Mapper     
    */

    //Comparator class to perform the sorting of the composite keys (keyword:count)
    //Start class CompositeKeyComparator
    public static class CompositeKeyComparator extends WritableComparator {

	    protected CompositeKeyComparator() {
		    super(CompKey.class, true);
	    }
	
	    //@SuppressWarnings("rawtypes")
	    @Override
	    public int compare(WritableComparable w1, WritableComparable w2) {
		    CompKey k1 = (CompKey)w1;
		    CompKey k2 = (CompKey)w2;
		
		    int result = k1.getKeyword().compareTo(k2.getKeyword());
		    if(0 == result) {
			  result = -1* k1.getCount().compareTo(k2.getCount());
		    }
		    return result;
	     }
    }
    //End class CompositeKeyComparator

    //The primary key grouping comparator will group values based on the primary key (keyword)
    //Start class PrimaryKeyGroupComparator
    public static class PrimaryKeyGroupingComparator extends WritableComparator {

	    protected PrimaryKeyGroupingComparator() {
		    super(CompKey.class, true);
	    }
	
	    //@SuppressWarnings("rawtypes")
	    @Override
	    public int compare(WritableComparable w1, WritableComparable w2) {
		    CompKey k1 = (CompKey)w1;
		    CompKey k2 = (CompKey)w2;
		
		return k1.getKeyword().compareTo(k2.getKeyword());
	    }
    }
    //End class PrimaryKeyGroupComparator

    //The primary key partitioner sends values with same primary key to the same reducer
    //Start class PrimaryKeyPartitioner
    public static class PrimaryKeyPartitioner extends Partitioner<CompKey, DoubleWritable> {

	    @Override
	    public int getPartition(CompKey key, DoubleWritable val, int numPartitions) {
		    int hash = key.getKeyword().hashCode();
		    int partition = hash % numPartitions;
		    return partition;
	    }
    }
    //End class PrimaryKeyPartitioner

    //2nd Mapper
    //Input : Output from 1st reducer, containing composite keys (state:keyword), values (count)
    //Output: Composite key (keyword:count), value (state)
    //Start class TokenizerMapper2
    public static class TokenizerMapper2
       extends Mapper<Object, Text, CompKey, Text>{

        private Text word = new Text();
        private Text state = new Text();
        private String[] str;
        private String[] tokens;
        private Long count;
        private CompKey skey = new CompKey();
    
        public void map(Object key, Text value, Context context
                    ) throws IOException, InterruptedException {
           //Parse each line from 1st reducer
           StringTokenizer itr = new StringTokenizer(value.toString(), "\n");
           while (itr.hasMoreTokens()) {
      	      //tokenize composite key and count value
      	      tokens = itr.nextToken().toString().split("\t");
      	      //split the composite key into state and keyword
      	      str = tokens[0].toString().split(":");
      	      //state
      	      state.set(str[0]);
      	      //keyword
      	      word.set(str[1]);
      	      //count
      	      count = Long.parseLong(tokens[1].trim());
      	      //create composite key (keyword:count)
              skey.SetCompKey(word.toString(),count);
              //write composite key (keyword:count) and value (state)
              context.write(skey, state);
          }
        }
    }
    //End class TokenizerMapper2
  
    //2nd Reducer
    //write the composite key (keyword:count), value (state) pairs into the output file
    //Start class KeywordReducer2
    public static class KeywordReducer2
       extends Reducer<CompKey, Text,CompKey,Text> {
        private IntWritable result = new IntWritable();

        public void reduce(CompKey key, Iterable<Text> values,
                       Context context
                       ) throws IOException, InterruptedException {
            //print top 3 states name in each category
            int i = 0;
            for (Text val : values) {
               if(i >= 3) 
                   break;
               context.write(key, val);
               i++;
            }
        }
    }
    //End class KeywordReducer2

    //Driver method
    //Start Driver
    public static void main(String[] args) throws Exception {
    	
      //Job #1, reads input state files, writes [(state:keyword), count] into 'tempop' file	
      Configuration conf = new Configuration();
      Job job1 = Job.getInstance(conf, "word count");
      job1.setJarByClass(WordCount.class);
      job1.setMapperClass(TokenizerMapper.class);
      job1.setCombinerClass(IntSumReducer.class);
      job1.setReducerClass(IntSumReducer.class);
      job1.setOutputKeyClass(CompositeGroupKey.class);
      job1.setOutputValueClass(IntWritable.class);
      FileInputFormat.addInputPath(job1, new Path(args[0]));
      FileOutputFormat.setOutputPath(job1, new Path("output1"));
      job1.waitForCompletion(true);
    
      //Job #2, reads 'output1' file, write sorted key (keyword:count), state into outout file
      Configuration conf2 = new Configuration();
      Job job2 = Job.getInstance(conf2, "word count");
      job2.setJarByClass(WordCount.class);
      job2.setMapperClass(TokenizerMapper2.class);
      job2.setCombinerClass(KeywordReducer2.class);
      job2.setReducerClass(KeywordReducer2.class);
      job2.setOutputKeyClass(CompKey.class);
      job2.setOutputValueClass(Text.class);
      //for sorting 2nd mapper output based on composite key
      job2.setPartitionerClass(PrimaryKeyPartitioner.class);
      job2.setGroupingComparatorClass(PrimaryKeyGroupingComparator.class);
      job2.setSortComparatorClass(CompositeKeyComparator.class);
      
      FileInputFormat.addInputPath(job2, new Path("output1"));
      FileOutputFormat.setOutputPath(job2, new Path(args[1]));
      System.exit(job2.waitForCompletion(true) ? 0 : 1);
    }
    //End Driver
}
