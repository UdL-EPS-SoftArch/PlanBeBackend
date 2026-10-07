---
name: sdd-cucumber-steps
description: Implement Cucumber step definitions (*StepDefs.java) that test Spring Data REST endpoints using MockMvc and Jackson.
---

# SDD Cucumber Steps Skill

This skill guides AI agents to implement clean, concise Cucumber step definitions (`*StepDefs.java`) for Spring Data REST entities. Avoid AmbiguousStepDefinitions errors by checking if steps already implemented in other *StepDefs.java files

## Conventions & Rules

1. **Inject Base StepDefs & Repositories**:
   Do NOT re-declare Spring test annotations (`@SpringBootTest`, `@ContextConfiguration`, etc.) in child step classes. Reuse `StepDefs.java`.
   ```java
   public class <Entity>StepDefs {
       private final StepDefs stepDefs;
       private final UserRepository userRepository;
       private final <Entity>Repository entityRepository;

       public <Entity>StepDefs(StepDefs stepDefs, UserRepository userRepository, <Entity>Repository entityRepository) {
           this.stepDefs = stepDefs;
           this.userRepository = userRepository;
           this.entityRepository = entityRepository;
       }
   }
   ```

2. **HTTP MockMvc Calls**:
   - Authentication: Append `.with(AuthenticationStepDefs.authenticate())` to `MockMvc` requests.
   - Set Content-Type and Accept headers (`MediaType.APPLICATION_JSON`).
   - Store output in `stepDefs.result`.

3. **Step Definition Template**:
   ```java
   @Given("There is a <entity> with name {string} owned by {string}")
   public void thereIsAnEntity(String name, String ownerUsername) {
       User owner = userRepository.findById(ownerUsername).orElseThrow();
       <Entity> entity = new <Entity>();
       entity.setName(name);
       entity.setOwnedBy(owner);
       entityRepository.save(entity);
   }

   @When("I create a new <entity> with name {string}")
   public void iCreateNewEntity(String name) throws Throwable {
       <Entity> entity = new <Entity>();
       entity.setName(name);

       stepDefs.result = stepDefs.mockMvc.perform(
               post("/<entities>")
                   .contentType(MediaType.APPLICATION_JSON)
                   .content(stepDefs.mapper.writeValueAsString(entity))
                   .characterEncoding(StandardCharsets.UTF_8)
                   .accept(MediaType.APPLICATION_JSON)
                   .with(AuthenticationStepDefs.authenticate()))
           .andDo(print());
   }

   @When("I retrieve the <entity> with name {string}")
   public void iRetrieveEntity(String name) throws Throwable {
       <Entity> entity = entityRepository.findByName(name).stream().findFirst().orElseThrow();
       stepDefs.result = stepDefs.mockMvc.perform(
               get(entity.getUri())
                   .accept(MediaType.APPLICATION_JSON)
                   .with(AuthenticationStepDefs.authenticate()))
           .andDo(print());
   }
   ```
