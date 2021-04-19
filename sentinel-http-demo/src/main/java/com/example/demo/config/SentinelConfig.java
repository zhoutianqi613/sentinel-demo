package com.example.demo.config;

import com.alibaba.csp.sentinel.EntryType;
import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRule;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRuleManager;
import com.alibaba.csp.sentinel.slots.block.flow.ClusterFlowConfig;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;
import com.alibaba.csp.sentinel.slots.system.SystemRule;
import com.alibaba.csp.sentinel.slots.system.SystemRuleManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.util.ClassUtils;
import org.springframework.util.RouteMatcher;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.*;

@Configuration
public class SentinelConfig {

    @Autowired
    private ApplicationContext applicationContext;

    private static final String BASE_PACKAGE = "com.example.demo";
    private static final String RESOURCE_PATTERN = "/**/*.class";

    @PostConstruct
    public void init() {
        System.out.println("run into SentinelConfig::init() ...");

        final List<SentinelResource> resources = scanSentinelResourceMethod(BASE_PACKAGE);

        for (SentinelResource resource: resources) {
            String resourceValue = resource.value();
            /* 设置熔断降级规则 */
            initDegradeRule(resourceValue);

            /* 设置限流、信号量隔离 */
            initFlowRule(resourceValue);

        }

        /* 设置系统自适应保护 */
        initSystemRule();
    }

    private List<SentinelResource> scanSentinelResourceMethod(String packageName) {
        List<SentinelResource> sentinelResourceMethod = new ArrayList<>();
        ResourcePatternResolver resourcePatternResolver = new PathMatchingResourcePatternResolver();
        try {
            String pattern = ResourcePatternResolver.CLASSPATH_ALL_URL_PREFIX
                    + ClassUtils.convertClassNameToResourcePath(packageName)+ RESOURCE_PATTERN;
            Resource[] resources = resourcePatternResolver.getResources(pattern);
            MetadataReaderFactory readerfactory = new CachingMetadataReaderFactory(resourcePatternResolver);

            if (resources.length == 0) {
                return sentinelResourceMethod;
            }
            for (Resource resource: resources) {
                MetadataReader reader = readerfactory.getMetadataReader(resource);
                String className = reader.getClassMetadata().getClassName();
                Class<?> clazz = Class.forName(className);
                Optional<Method[]> methods = Optional.of(clazz.getMethods());
                methods.ifPresent(x -> {
                    for (Method method: x) {
                        SentinelResource annotation = method.getAnnotation(SentinelResource.class);
                        if(annotation != null) {
                            sentinelResourceMethod.add(annotation);
                        }
                    }
                });
            }
        } catch( ClassNotFoundException | IOException ignored) {

        }

        return sentinelResourceMethod;
    }

    private void initDegradeRule(String resource) {
        /* 以平均响应时间作为降级规则，在5秒中平均响应时间大于10S, 则触发降级 */
        DegradeRule ratioDegradeRule = new DegradeRule(resource)
                .setCount(10000)
                .setGrade(RuleConstant.DEGRADE_GRADE_RT)
                .setTimeWindow(5);

        /* 以异常比例作为降级规则，在5秒内异常比例大于80%, 则触发降级 */
        DegradeRule degradeRule = new DegradeRule(resource)
                .setCount(0.8)
                .setGrade(RuleConstant.DEGRADE_GRADE_EXCEPTION_RATIO)
                .setTimeWindow(5)
                //.setMinRequestAmount(10)
                ;
        /* 以异常数作为降级规则：在5秒内,异常数大于等于1, 则触发降级*/
        DegradeRule degradeRuleCount = new DegradeRule(resource)
                .setCount(10)
                .setGrade(RuleConstant.DEGRADE_GRADE_EXCEPTION_COUNT)
                .setTimeWindow(5)
                ;

        List<DegradeRule> degradeRules = new ArrayList<>();
        degradeRules.add(ratioDegradeRule);
        degradeRules.add(degradeRule);
        degradeRules.add(degradeRuleCount);
        degradeRules.addAll(DegradeRuleManager.getRules());
        DegradeRuleManager.loadRules(degradeRules);
    }

    private void initFlowRule(String resource) {
        /* 以QPS作为流控规则,其每秒不得超过10次调用, */
        FlowRule qpsFlowRule = new FlowRule(resource)
                .setCount(10)
                .setGrade(RuleConstant.FLOW_GRADE_QPS)
                .setStrategy(RuleConstant.STRATEGY_DIRECT)                   // default
                .setControlBehavior(RuleConstant.CONTROL_BEHAVIOR_DEFAULT)   // default
                //.setClusterConfig(new ClusterFlowConfig());
                ;
        qpsFlowRule.setLimitApp("default");

        /* 以限制线程并发数LOW_GRADE_THREAD为流控规则,其最大不能超过10 */
        FlowRule threadFlowRule = new FlowRule(resource)
                .setCount(10)
                .setGrade(RuleConstant.FLOW_GRADE_THREAD)
                .setStrategy(RuleConstant.STRATEGY_DIRECT)                   // default
                .setControlBehavior(RuleConstant.CONTROL_BEHAVIOR_DEFAULT)   // default
                ;
        threadFlowRule.setLimitApp("default");

        List<FlowRule> flowRules = new ArrayList<>();
        flowRules.add(qpsFlowRule);
        flowRules.add(threadFlowRule);
        flowRules.addAll(FlowRuleManager.getRules());
        FlowRuleManager.loadRules(flowRules);
    }

    private void initSystemRule() {
        List<SystemRule> rules = new ArrayList<>();
        SystemRule systemRule = new SystemRule();
        systemRule.setHighestSystemLoad(100);        // 最大的load
        systemRule.setMaxThread(20);                // 入口流量的最大并发数
        systemRule.setAvgRt(10*1000);               // 入口流量的平均响应时间,单位：毫秒
        systemRule.setQps(100);                     // 所有入口资源的QPS
        rules.add(systemRule);
        SystemRuleManager.loadRules(rules);
    }
}
