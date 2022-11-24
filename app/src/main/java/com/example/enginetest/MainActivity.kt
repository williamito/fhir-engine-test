package com.example.enginetest

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.coroutineScope
import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.context.FhirVersionEnum
import com.google.android.fhir.DatabaseErrorStrategy
import com.google.android.fhir.FhirEngineConfiguration
import com.google.android.fhir.FhirEngineProvider
import com.google.android.fhir.get
import com.google.android.fhir.workflow.FhirOperator
import kotlinx.coroutines.launch
import org.hl7.fhir.r4.model.Encounter
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.PlanDefinition

class MainActivity : AppCompatActivity() {
  private val fhirEngine by lazy {FhirEngineProvider.getInstance(this)}

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    val jsonParser = FhirContext.forCached(FhirVersionEnum.R4).newJsonParser()

    FhirEngineProvider.init(
      FhirEngineConfiguration(
        enableEncryptionIfSupported = true,
        DatabaseErrorStrategy.RECREATE_AT_OPEN
      )
    )

    val fhirOperator = FhirOperator(
      fhirContext = FhirContext.forCached(FhirVersionEnum.R4),
      fhirEngine = FhirEngineProvider.getInstance(this)
    )

    val planDefinitionStr =
      """
        {
          "resourceType": "PlanDefinition",
          "id": "Test-PlanDefinition",
          "status": "active",
          "action": [
            {
              "definitionCanonical": "http://example.org/ActivityDefinition/ADTest"
            }
         ]
        }
      """.trimIndent()

    val planDefinitionConditionStr =
      """
        {
          "resourceType": "PlanDefinition",
          "id": "Test-PlanDefinitionCondition",
          "status": "active",
          "action": [
            {
              "condition": [
                {
                  "kind": "applicability",
                  "expression": {
                    "language": "text/fhirpath",
                    "expression": "true"
                  }
                }
              ],
              "definitionCanonical": "http://example.org/ActivityDefinition/ADTest"
            }
         ]
        }
      """.trimIndent()

    val patientStr =
      """
      {
        "resourceType": "Patient",
        "id": "Test-Patient",
        "gender": "female"
      }
      """

    val encounterStr =
      """
      {
        "resourceType": "Encounter",
        "id": "Test-Encounter",
        "status": "in-progress",
        "subject": {
          "reference": "Patient/Test-Patient"
        }
      }
      """.trimIndent()

    lifecycle.coroutineScope.launch {
      fhirEngine.create(jsonParser.parseResource(planDefinitionStr) as PlanDefinition)
      fhirEngine.create(jsonParser.parseResource(planDefinitionConditionStr) as PlanDefinition)
      fhirEngine.create(jsonParser.parseResource(patientStr) as Patient)
      fhirEngine.create(jsonParser.parseResource(encounterStr) as Encounter)


      val planDefinitionResource = fhirEngine.get<PlanDefinition>("Test-PlanDefinition")
      val patientResource = fhirEngine.get<Patient>("Test-Patient")
      val encounterResource = fhirEngine.get<Encounter>("Test-Encounter")
      Log.d("planDefinition", jsonParser.encodeResourceToString(planDefinitionResource))
      Log.d("patient", jsonParser.encodeResourceToString(patientResource))
      Log.d("encounter", jsonParser.encodeResourceToString(encounterResource))

      val carePlan = fhirOperator.generateCarePlan(
        planDefinitionId = "Test-PlanDefinition",
        patientId = "Test-Patient",
        encounterId = "Test-Encounter"
      )
      Log.d("carePlan", jsonParser.encodeResourceToString(carePlan))

      // Crashes here
      val carePlan2 = fhirOperator.generateCarePlan(
        planDefinitionId = "Test-PlanDefinitionCondition",
        patientId = "Test-Patient",
        encounterId = "Test-Encounter"
      )
      Log.d("carePlan2", jsonParser.encodeResourceToString(carePlan2))
    }

    setContentView(R.layout.activity_main)
  }
}