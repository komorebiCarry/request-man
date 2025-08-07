package com.ljh.request.requestman.util;

import cn.hutool.json.JSONUtil;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.project.Project;
import com.ljh.request.requestman.model.Environment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 项目设置管理器，用于管理项目级别的设置。
 * 支持前置URL、全局认证、全局变量等项目特定配置。
 *
 * @author leijianhui
 * @Description 项目设置管理器，管理项目级别的配置信息。
 * @date 2025/01/27 16:00
 */
public class ProjectSettingsManager {

    /**
     * 项目变量池，key为项目名称，value为变量Map
     */
    private static final Map<String, Map<String, String>> projectVariableMap = new HashMap<>();

    /**
     * 项目设置缓存，key为项目名称，value为设置Map
     */
    private static final Map<String, Map<String, String>> projectSettingsMap = new HashMap<>();

    /**
     * 项目环境缓存，key为项目名称，value为环境列表
     */
    private static final Map<String, List<Environment>> projectEnvironmentsMap = new HashMap<>();

    /**
     * 持久化key前缀
     */
    private static final String PROJECT_VARS_PREFIX = "requestman.project.vars.";
    private static final String PROJECT_SETTINGS_PREFIX = "requestman.project.settings.";
    private static final String PROJECT_ENVIRONMENTS_PREFIX = "requestman.project.environments.";

    /**
     * 设置项key
     */
    public static final String PRE_URL_KEY = "preUrl";
    public static final String GLOBAL_AUTH_KEY = "globalAuth";
    public static final String CURRENT_ENVIRONMENT_KEY = "currentEnvironment";

    /**
     * 获取项目设置值
     *
     * @param project      项目对象
     * @param key          设置项key
     * @param defaultValue 默认值
     * @return 设置值
     */
    public static String getProjectSetting(Project project, String key, String defaultValue) {
        if (project == null) {
            return defaultValue;
        }

        String projectName = project.getName();
        Map<String, String> settings = projectSettingsMap.computeIfAbsent(projectName, k -> loadProjectSettings(project));
        return settings.getOrDefault(key, defaultValue);
    }

    /**
     * 设置项目设置值
     *
     * @param project 项目对象
     * @param key     设置项key
     * @param value   设置值
     */
    public static void setProjectSetting(Project project, String key, String value) {
        if (project == null) {
            return;
        }

        String projectName = project.getName();
        Map<String, String> settings = projectSettingsMap.computeIfAbsent(projectName, k -> loadProjectSettings(project));
        settings.put(key, value);
        persistProjectSettings(project, settings);
    }

    /**
     * 获取项目变量值
     *
     * @param project 项目对象
     * @param varName 变量名
     * @return 变量值，若不存在返回null
     */
    public static String getProjectVariable(Project project, String varName) {
        if (project == null) {
            return null;
        }

        String projectName = project.getName();
        Map<String, String> variables = projectVariableMap.computeIfAbsent(projectName, k -> loadProjectVariables(project));
        return variables.get(varName);
    }

    /**
     * 设置项目变量值
     *
     * @param project  项目对象
     * @param varName  变量名
     * @param varValue 变量值
     */
    public static void setProjectVariable(Project project, String varName, String varValue) {
        if (project == null || varName == null || varName.trim().isEmpty()) {
            return;
        }

        String projectName = project.getName();
        Map<String, String> variables = projectVariableMap.computeIfAbsent(projectName, k -> loadProjectVariables(project));
        variables.put(varName, varValue != null ? varValue : "");
        persistProjectVariables(project, variables);
    }

    /**
     * 删除项目变量
     *
     * @param project 项目对象
     * @param varName 变量名
     */
    public static void removeProjectVariable(Project project, String varName) {
        if (project == null) {
            return;
        }

        String projectName = project.getName();
        Map<String, String> variables = projectVariableMap.computeIfAbsent(projectName, k -> loadProjectVariables(project));
        variables.remove(varName);
        persistProjectVariables(project, variables);
    }

    /**
     * 获取项目所有变量（只读Map）
     *
     * @param project 项目对象
     * @return 变量Map快照
     */
    public static Map<String, String> getAllProjectVariables(Project project) {
        if (project == null) {
            return Collections.emptyMap();
        }

        String projectName = project.getName();
        Map<String, String> variables = projectVariableMap.computeIfAbsent(projectName, k -> loadProjectVariables(project));
        return Collections.unmodifiableMap(variables);
    }

    /**
     * 清空项目所有变量
     *
     * @param project 项目对象
     */
    public static void clearProjectVariables(Project project) {
        if (project == null) {
            return;
        }

        String projectName = project.getName();
        Map<String, String> variables = projectVariableMap.computeIfAbsent(projectName, k -> loadProjectVariables(project));
        variables.clear();
        persistProjectVariables(project, variables);
    }

    /**
     * 判断项目变量是否存在
     *
     * @param project 项目对象
     * @param varName 变量名
     * @return 是否存在
     */
    public static boolean containsProjectVariable(Project project, String varName) {
        if (project == null) {
            return false;
        }

        String projectName = project.getName();
        Map<String, String> variables = projectVariableMap.computeIfAbsent(projectName, k -> loadProjectVariables(project));
        return variables.containsKey(varName);
    }

    /**
     * 加载项目设置
     *
     * @param project 项目对象
     * @return 设置Map
     */
    private static Map<String, String> loadProjectSettings(Project project) {
        String projectName = project.getName();
        String key = PROJECT_SETTINGS_PREFIX + projectName;
        String json = PropertiesComponent.getInstance().getValue(key, "{}");

        Map<String, String> settings = new HashMap<>();
        try {
            Map<String, Object> map = JSONUtil.parseObj(json);
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                settings.put(entry.getKey(), entry.getValue() != null ? entry.getValue().toString() : "");
            }
        } catch (Exception e) {
            // 解析失败时使用空Map
        }

        return settings;
    }

    /**
     * 持久化项目设置
     *
     * @param project  项目对象
     * @param settings 设置Map
     */
    private static void persistProjectSettings(Project project, Map<String, String> settings) {
        String projectName = project.getName();
        String key = PROJECT_SETTINGS_PREFIX + projectName;
        String json = JSONUtil.toJsonStr(settings);
        PropertiesComponent.getInstance().setValue(key, json);
    }

    /**
     * 加载项目变量
     *
     * @param project 项目对象
     * @return 变量Map
     */
    private static Map<String, String> loadProjectVariables(Project project) {
        String projectName = project.getName();
        String key = PROJECT_VARS_PREFIX + projectName;
        String json = PropertiesComponent.getInstance().getValue(key, "{}");

        Map<String, String> variables = new HashMap<>();
        try {
            Map<String, Object> map = JSONUtil.parseObj(json);
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                variables.put(entry.getKey(), entry.getValue() != null ? entry.getValue().toString() : "");
            }
        } catch (Exception e) {
            // 解析失败时使用空Map
        }

        return variables;
    }

    /**
     * 持久化项目变量
     *
     * @param project   项目对象
     * @param variables 变量Map
     */
    private static void persistProjectVariables(Project project, Map<String, String> variables) {
        String projectName = project.getName();
        String key = PROJECT_VARS_PREFIX + projectName;
        String json = JSONUtil.toJsonStr(variables);
        PropertiesComponent.getInstance().setValue(key, json);
    }

    /**
     * 清理项目缓存（项目关闭时调用）
     *
     * @param project 项目对象
     */
    public static void clearProjectCache(Project project) {
        if (project == null) {
            return;
        }

        String projectName = project.getName();
        projectVariableMap.remove(projectName);
        projectSettingsMap.remove(projectName);
    }

    /**
     * 获取项目前置URL
     *
     * @param project 项目对象
     * @return 前置URL
     */
    public static String getProjectPreUrl(Project project) {
        return getProjectSetting(project, PRE_URL_KEY, "");
    }

    /**
     * 设置项目前置URL
     *
     * @param project 项目对象
     * @param preUrl  前置URL
     */
    public static void setProjectPreUrl(Project project, String preUrl) {
        setProjectSetting(project, PRE_URL_KEY, preUrl);
    }

    /**
     * 获取项目全局认证信息
     *
     * @param project 项目对象
     * @return 全局认证信息
     */
    public static String getProjectGlobalAuth(Project project) {
        return getProjectSetting(project, GLOBAL_AUTH_KEY, "");
    }

    /**
     * 设置项目全局认证信息
     *
     * @param project    项目对象
     * @param globalAuth 全局认证信息
     */
    public static void setProjectGlobalAuth(Project project, String globalAuth) {
        setProjectSetting(project, GLOBAL_AUTH_KEY, globalAuth);
    }

    // ==================== 环境管理方法 ====================

    /**
     * 获取项目所有环境
     *
     * @param project 项目对象
     * @return 环境列表
     */
    public static List<Environment> getAllEnvironments(Project project) {
        if (project == null) {
            return new ArrayList<>();
        }
        String projectName = project.getName();
        List<Environment> environments = projectEnvironmentsMap.computeIfAbsent(projectName, k -> loadProjectEnvironments(project));

        // 如果环境列表为空，初始化默认环境
        if (environments.isEmpty()) {
            initializeDefaultEnvironments(project);
            environments = projectEnvironmentsMap.get(projectName);
        }

        return environments;
    }

    /**
     * 添加环境
     *
     * @param project     项目对象
     * @param environment 环境对象
     */
    public static void addEnvironment(Project project, Environment environment) {
        if (project == null || environment == null) {
            return;
        }

        String projectName = project.getName();
        List<Environment> environments = projectEnvironmentsMap.computeIfAbsent(projectName, k -> loadProjectEnvironments(project));

        environments.add(environment);
        persistProjectEnvironments(project, environments);
    }

    /**
     * 更新环境
     *
     * @param project     项目对象
     * @param environment 环境对象
     */
    public static void updateEnvironment(Project project, Environment environment) {
        if (project == null || environment == null) {
            return;
        }

        String projectName = project.getName();
        List<Environment> environments = projectEnvironmentsMap.computeIfAbsent(projectName, k -> loadProjectEnvironments(project));

        for (int i = 0; i < environments.size(); i++) {
            if (environments.get(i).getId().equals(environment.getId())) {
                environments.set(i, environment);
                break;
            }
        }

        persistProjectEnvironments(project, environments);
    }

    /**
     * 删除环境
     *
     * @param project       项目对象
     * @param environmentId 环境ID
     */
    public static void removeEnvironment(Project project, String environmentId) {
        if (project == null || environmentId == null) {
            return;
        }

        String projectName = project.getName();
        List<Environment> environments = projectEnvironmentsMap.computeIfAbsent(projectName, k -> loadProjectEnvironments(project));

        environments.removeIf(env -> env.getId().equals(environmentId));
        persistProjectEnvironments(project, environments);
    }

    // 移除默认环境相关方法

    /**
     * 根据ID获取环境
     *
     * @param project       项目对象
     * @param environmentId 环境ID
     * @return 环境对象，如果不存在则返回null
     */
    public static Environment getEnvironmentById(Project project, String environmentId) {
        if (project == null || environmentId == null) {
            return null;
        }

        List<Environment> environments = getAllEnvironments(project);
        return environments.stream()
                .filter(env -> env.getId().equals(environmentId))
                .findFirst()
                .orElse(null);
    }

    /**
     * 获取当前选中的环境ID
     *
     * @param project 项目对象
     * @return 当前环境ID
     */
    public static String getCurrentEnvironmentId(Project project) {
        if (project == null) {
            return null;
        }

        String currentId = getProjectSetting(project, CURRENT_ENVIRONMENT_KEY, "");
        if (currentId.isEmpty()) {
            // 如果没有选中环境，返回第一个环境
            List<Environment> environments = getAllEnvironments(project);
            return environments.isEmpty() ? null : environments.get(0).getId();
        }
        return currentId;
    }

    /**
     * 设置当前选中的环境ID
     *
     * @param project       项目对象
     * @param environmentId 环境ID
     */
    public static void setCurrentEnvironmentId(Project project, String environmentId) {
        setProjectSetting(project, CURRENT_ENVIRONMENT_KEY, environmentId);
    }

    /**
     * 获取当前环境的前置URL
     *
     * @param project 项目对象
     * @return 前置URL
     */
    public static String getCurrentEnvironmentPreUrl(Project project) {
        String currentId = getCurrentEnvironmentId(project);
        if (currentId == null) {
            return "";
        }

        Environment env = getEnvironmentById(project, currentId);
        return env != null ? env.getPreUrl() : "";
    }

    /**
     * 获取当前环境的全局认证（现在从项目级别获取）
     *
     * @param project 项目对象
     * @return 全局认证信息
     */
    public static String getCurrentEnvironmentGlobalAuth(Project project) {
        // 全局认证现在从项目级别获取，不再从环境中获取
        return getProjectGlobalAuth(project);
    }

    /**
     * 加载项目环境配置
     *
     * @param project 项目对象
     * @return 环境列表
     */
    private static List<Environment> loadProjectEnvironments(Project project) {
        String projectName = project.getName();
        String key = PROJECT_ENVIRONMENTS_PREFIX + projectName;
        String json = PropertiesComponent.getInstance().getValue(key, "[]");

        List<Environment> environments = new ArrayList<>();
        try {
            List<?> list = JSONUtil.parseArray(json).toList(Object.class);
            for (Object item : list) {
                if (item instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> map = (Map<String, Object>) item;
                    Environment env = new Environment();
                    env.setId((String) map.get("id"));
                    env.setName((String) map.get("name"));
                    env.setPreUrl((String) map.get("preUrl"));
                    // 全局认证现在从项目级别获取，不再从环境中加载
                    env.setCreateTime(((Number) map.get("createTime")).longValue());
                    env.setUpdateTime(((Number) map.get("updateTime")).longValue());
                    environments.add(env);
                }
            }
        } catch (Exception e) {
            // 解析失败时使用空列表
        }

        return environments;
    }

    /**
     * 持久化项目环境配置
     *
     * @param project      项目对象
     * @param environments 环境列表
     */
    private static void persistProjectEnvironments(Project project, List<Environment> environments) {
        String projectName = project.getName();
        String key = PROJECT_ENVIRONMENTS_PREFIX + projectName;
        String json = JSONUtil.toJsonStr(environments);
        PropertiesComponent.getInstance().setValue(key, json);
    }

    /**
     * 清理项目环境缓存（项目关闭时调用）
     *
     * @param project 项目对象
     */
    public static void clearProjectEnvironmentCache(Project project) {
        if (project == null) {
            return;
        }

        String projectName = project.getName();
        projectEnvironmentsMap.remove(projectName);
    }

    /**
     * 初始化默认环境配置
     *
     * @param project 项目对象
     */
    private static void initializeDefaultEnvironments(Project project) {
        if (project == null) {
            return;
        }

        String projectName = project.getName();
        List<Environment> environments = new ArrayList<>();

        // 创建开发环境
        Environment devEnv = new Environment("开发环境", "");
        environments.add(devEnv);

        // 创建测试环境
        Environment testEnv = new Environment("测试环境", "");
        environments.add(testEnv);

        // 创建正式环境
        Environment prodEnv = new Environment("正式环境", "");
        environments.add(prodEnv);

        // 保存到缓存和持久化
        projectEnvironmentsMap.put(projectName, environments);
        persistProjectEnvironments(project, environments);
    }
}