package com.sika.code.batch.config;

import com.sika.code.batch.person.*;
import com.sika.code.common.json.util.JSONUtil;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.launch.support.SimpleJobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.support.JobRepositoryFactoryBean;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.BeanPropertyItemSqlParameterSourceProvider;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.validator.Validator;
import org.springframework.batch.support.DatabaseType;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.transaction.PlatformTransactionManager;

import javax.naming.ServiceUnavailableException;
import javax.sql.DataSource;
import java.util.List;

/**
 * @author daiqi
 * @create 2019-09-10 0:31
 */
@Configuration
@EnableBatchProcessing
public class CsvConfig {

    /**
     * ItemReader定义,用来读取数据
     * 1，使用FlatFileItemReader读取文件
     * 2，使用FlatFileItemReader的setResource方法设置csv文件的路径
     * 3，对此对cvs文件的数据和领域模型类做对应映射
     * 使用@StepScope注解必须返回为实现类，因为是通过代理生成相应的bean
     * @return
     * @throws Exception
     */
    @Bean
    @StepScope
    public FlatFileItemReader<PersonEntity> reader(@Value("#{jobParameters[namestr]}") String namestr)throws Exception {
        FlatFileItemReader<PersonEntity> reader = new FlatFileItemReader<>();
        reader.setResource(new ClassPathResource("person.csv"));
        reader.setLinesToSkip(1);
        reader.setSkippedLinesCallback(new CsvLineCallbackHandler());
//        String [] names = {"name","age","nation","address"};
        List<String> names1 = JSONUtil.parseArray(namestr, String.class);
        String [] names = new String[names1.size()];
        names1.toArray(names);
        String delimited = "|";
        reader.setLineMapper(BatchUtil.lineMapper(PersonEntity.class, delimited, names));
        return reader;
    }

    /**
     * ItemProcessor定义，用来处理数据
     * @return
     */
    @Bean
    @StepScope
    public ItemProcessor<PersonEntity,PersonEntity> processor(@Value("#{jobParameters[namestr]}") String namestr){
        //使用我们自定义的ItemProcessor的实现CsvItemProcessor
        CsvItemProcessor processor = new CsvItemProcessor();
        //为processor指定校验器为CsvBeanValidator()
        processor.setValidator(csvBeanValidator());
        return processor;
    }

    /**
     * ItemWriter定义，用来输出数据
     * spring能让容器中已有的Bean以参数的形式注入，Spring Boot已经为我们定义了dataSource
     * @param dataSource
     * @return
     */
    @Bean
    @StepScope
    public ItemWriter<PersonEntity> writer(@Value("#{jobParameters[namestr]}") String namestr, @Qualifier("dataSource") DataSource dataSource){
        ItemWriter<PersonEntity> writer = new ItemWriter<PersonEntity>() {
            @Override
            public void write(List<? extends PersonEntity> items) throws Exception {

            }
        };
        return writer;
//        JdbcBatchItemWriter<PersonEntity> writer = new JdbcBatchItemWriter<>();
//        //我们使用JDBC批处理的JdbcBatchItemWriter来写数据到数据库
//        writer.setItemSqlParameterSourceProvider(new BeanPropertyItemSqlParameterSourceProvider<>());
//        String sql = "insert into person "+" (name,age,nation,address) "
//                +" values(:name,:age,:nation,:address)";
//        //在此设置要执行批处理的SQL语句
//        writer.setSql(sql);
//        writer.setDataSource(dataSource);
//        return writer;
    }

    /**
     * JobRepository，用来注册Job的容器
     * jobRepositor的定义需要dataSource和transactionManager，Spring Boot已为我们自动配置了
     * 这两个类，Spring可通过方法注入已有的Bean
     * @param dataSource
     * @param transactionManager
     * @return
     * @throws Exception
     */
    @Bean
    public JobRepository jobRepository(@Qualifier("dataSource") DataSource dataSource, PlatformTransactionManager transactionManager)throws Exception{

        JobRepositoryFactoryBean jobRepositoryFactoryBean =
                new JobRepositoryFactoryBean();
        jobRepositoryFactoryBean.setDataSource(dataSource);
        jobRepositoryFactoryBean.setTransactionManager(transactionManager);
        jobRepositoryFactoryBean.setDatabaseType(DatabaseType.MYSQL.name());
        return jobRepositoryFactoryBean.getObject();
    }

    /**
     * JobLauncher定义，用来启动Job的接口
     * @param dataSource
     * @param transactionManager
     * @return
     * @throws Exception
     */
    @Bean
    public SimpleJobLauncher jobLauncher(@Qualifier("dataSource") DataSource dataSource, PlatformTransactionManager transactionManager)throws Exception{
        SimpleJobLauncher jobLauncher = new SimpleJobLauncher();
        jobLauncher.setJobRepository(jobRepository(dataSource, transactionManager));
        return jobLauncher;
    }

    /**
     * Job定义，我们要实际执行的任务，包含一个或多个Step
     * @param jobBuilderFactory
     * @param s1
     * @return
     */
    @Bean
    public Job importJob(JobBuilderFactory jobBuilderFactory, Step step){
        return jobBuilderFactory.get("importJob")
                .incrementer(new RunIdIncrementer())
                .flow(step)//为Job指定Step
                .end()
                .listener(csvJobListener())//绑定监听器csvJobListener
                .build();
    }

    /**
     *step步骤，包含ItemReader，ItemProcessor和ItemWriter
     * @param stepBuilderFactory
     * @param reader
     * @param writer
     * @param processor
     * @return
     */
    @Bean
    public Step step(StepBuilderFactory stepBuilderFactory, ItemReader<PersonEntity> reader, ItemWriter<PersonEntity> writer,
                      ItemProcessor<PersonEntity,PersonEntity> processor){
        return stepBuilderFactory
                .get("step")
                .<PersonEntity,PersonEntity>chunk(1000)//批处理每次提交65000条数据
                .reader(reader)//给step绑定reader
                .processor(processor)//给step绑定processor
                .writer(writer)//给step绑定writer
                .faultTolerant()
                .skipLimit(1)
                .skip(ServiceUnavailableException.class)
                .retryLimit(5)
                .retry(ServiceUnavailableException.class)
                .throttleLimit(10)
                .build();
    }

    @Bean
    public CsvJobListener csvJobListener(){
        return new CsvJobListener();
    }
    @Bean
    public Validator<PersonEntity> csvBeanValidator(){
        return new CsvBeanValidator<PersonEntity>();
    }
}