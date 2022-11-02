package demo;
// [START import]
import static java.lang.Math.max;

import com.google.ortools.Loader;
import com.google.ortools.sat.*;
import demo.bootstrap.DataGenerator;
import demo.domain.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
// [END import]

public class OrToolsJobApp {
    Integer horizon = 0;
    Map<String, TaskVariable> allTasks = new HashMap<>();
    Map<String, List<IntervalVar>> resourceToIntervals = new HashMap<>();
    CpModel model = new CpModel();
    List<Task> taskList = DataGenerator.generateTaskList();
    List<ResourceItem> resourceItems = DataGenerator.generateResources();
    LocalDateTime taskBeginTime = LocalDateTime.of(2022, 10, 1, 0, 0, 0);




    public static Schedule generateDemoData() {
        Schedule schedule = new Schedule();
//        List<ResourceItem> resourceItemList = DataGenerator.generateResources();
//        List<ManufacturerOrder> manufacturerOrders = DataGenerator.generateOrderList();
//        List<Task> taskList = DataGenerator.generateTaskList();
//        DataGenerator.sortTask(taskList);
//        List<Allocation> allocationList = DataGenerator.createAllocationList(taskList, manufacturerOrders);
//        schedule.setT        Loader.loadNativeLibraries();askList(taskList);
//        schedule.setAllocationList(allocationList);
//        schedule.setResourceList(resourceItemList);
////        schedule.setResourceRequirementList(null);
//        schedule.setManufacturerOrderList(manufacturerOrders);
        return schedule;
    }
    public Integer calculateHorizon(){
//        List<Task> taskList = DataGenerator.generateTaskList();
        for (Task task:taskList){
            horizon+= task.getHalfHourDuration();
        }
        return horizon;
    }
    //生成计划变量
    public void generateVariables() {
        for (Task task : taskList) {
            String suffix = "_" + task.getId();
            TaskVariable taskVariable = new TaskVariable();
            taskVariable.setStart(model.newIntVar(0, horizon, "start" + suffix));
            taskVariable.setEnd(model.newIntVar(0, horizon, "end" + suffix));
            taskVariable.setInterval(model.newIntervalVar(taskVariable.getStart(), LinearExpr.constant(task.getHalfHourDuration())
                    , taskVariable.getEnd(), "interval" + suffix));
            allTasks.put(task.getId(), taskVariable);
            resourceToIntervals.computeIfAbsent(task.getRequiredResourceId(), key -> new ArrayList<>());
            resourceToIntervals.get(task.getRequiredResourceId()).add(taskVariable.getInterval());
        }
    }
    //创建时间间隔不能重复的约束
    public void createConstraints(){
        for(ResourceItem resourceItem:resourceItems){
            List<IntervalVar> list = resourceToIntervals.get(resourceItem.getResourcePoolId());
            model.addNoOverlap(list);
        }
    }
    public void createRelatedLayerConstraints(){
        IntVar max = model.newIntVar(0, horizon,"max");
        for (Task task : taskList) {
            Integer unit = task.getUnit();
            if(unit==1){
                List<Integer> relatedLayer = task.getRelatedLayer();
                if(relatedLayer!=null){
                    String currentTaskId = task.getId();
                    String orderId = task.getOrderId();
                    List<String> processorTaskIds= taskList.stream().filter(task1 -> task1.getLayerNum()!=null && task1.getOrderId().equals(orderId) && relatedLayer.contains(task1.getLayerNum()))
                            .map(Task::getId).collect(Collectors.toList());

                    TaskVariable currentTaskVariable = allTasks.get(currentTaskId);
                    List<IntVar> processorEnds = new ArrayList<>();

                    processorTaskIds.forEach(i->{
                        IntVar end = allTasks.get(i).getEnd();
                        processorEnds.add(end);
                    });
                    model.addMaxEquality(max,processorEnds);
                    model.addGreaterOrEqual(currentTaskVariable.getStart(),max);
                }

            }
        }
    }

    //创建按照固定优先级顺序排列的顺序顺序约束
    public void createPrecedence(){
        IntVar minConstant = model.newConstant(1);
        IntVar maxConstant = model.newConstant(3);

        for (Task task : taskList) {
            Task nextTask = task.getNextTask();
            if(nextTask!=null){
                String preKey = task.getId();
                String nextKey = nextTask.getId();
                model.addGreaterOrEqual(allTasks.get(nextKey).getStart(), LinearExpr.weightedSum(new IntVar[]{allTasks.get(preKey).getEnd(),minConstant},new long[]{1,1}));
                model.addLessOrEqual(allTasks.get(nextKey).getStart(), LinearExpr.weightedSum(new IntVar[]{allTasks.get(preKey).getEnd(),maxConstant},new long[]{1,1}));

            }
        }
    }
    //定义目标函数和训练规则
    public void defineObjective(){
        IntVar objVar = model.newIntVar(0, horizon, "makespan");
        List<IntVar> ends = new ArrayList<>();
        for (Task task : taskList) {
            Task nextTask = task.getNextTask();
            if(nextTask==null){
                IntVar end = allTasks.get(task.getId()).getEnd();
                ends.add(end);
            }
        }
        model.addMaxEquality(objVar, ends);
        model.minimize(objVar);

    }
    //求解器配置
    public void solve() {
        CpSolver solver = new CpSolver();
        solver.getParameters().setLogSearchProgress(true);

        CpSolverStatus status = solver.solve(model);
        if (status == CpSolverStatus.OPTIMAL || status == CpSolverStatus.FEASIBLE) {
            System.out.println("Solution:");
            Map<String, List<AssignedTask>> assignedJobs = new HashMap<>();
            for (Task task : taskList) {
                String taskId = task.getId();
                String key = taskId;
//                System.out.println(key);
                AssignedTask assignedTask = new AssignedTask(
                        taskId, (int) solver.value(allTasks.get(key).getStart()), task.getHalfHourDuration(),0,0);
                assignedJobs.computeIfAbsent(task.getRequiredResourceId(), k -> new ArrayList<>());
                assignedJobs.get(task.getRequiredResourceId()).add(assignedTask);
            }

            String output = "";
            for (ResourceItem resourceItem : resourceItems) {
                Collections.sort(assignedJobs.get(resourceItem.getResourcePoolId()), new SortTasks());
                String solLineTasks = "" + resourceItem.getResourcePoolId() + ": ";
                String solLine = "           ";
                for (AssignedTask assignedTask : assignedJobs.get(resourceItem.getResourcePoolId())) {
                    String name = "" + assignedTask.getTaskId();
                    solLineTasks += String.format("%-15s", name);

                    String solTmp =
                            "[" + assignedTask.getStart() + "," + (assignedTask.getStart() + assignedTask.getDuration()) + "]";
                    // Add spaces to output to align columns.
                    solLine += String.format("%-15s", solTmp);
                }
                output += solLineTasks + "%n";
                output += solLine + "%n";
            }
            System.out.printf("Optimal Schedule Length: %f%n", solver.objectiveValue());
            System.out.printf(output);

        }else{
            System.out.println("No solution found.");
        }
    }


    public static void main(String[] args) {
        Loader.loadNativeLibraries();

        OrToolsJobApp orToolsJobApp = new OrToolsJobApp();
        orToolsJobApp.calculateHorizon();
        orToolsJobApp.generateVariables();
        orToolsJobApp.createConstraints();
//        orToolsJobApp.createRelatedLayerConstraints();
        orToolsJobApp.createPrecedence();
        orToolsJobApp.defineObjective();
        orToolsJobApp.solve();


    }
}
