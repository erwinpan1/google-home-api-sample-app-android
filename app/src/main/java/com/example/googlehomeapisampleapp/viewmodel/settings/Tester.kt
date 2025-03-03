
/* Copyright 2025 Google LLC

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    https://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package com.example.googlehomeapisampleapp.viewmodel.settings
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.googlehomeapisampleapp.HomeApp.Companion.supportedTraits
import com.google.home.Descriptor
import com.google.home.DeviceType
import com.google.home.DeviceTypeFactory
import com.google.home.HomeClient
import com.google.home.HomeDevice
import com.google.home.HomeException
import com.google.home.HomeObjectsFlow
import com.google.home.Id
import com.google.home.Structure
import com.google.home.Trait
import com.google.home.TraitFactory
import com.google.home.automation.Action
import com.google.home.automation.Automation
import com.google.home.automation.BaseAutomation
import com.google.home.automation.BinaryExpression
import com.google.home.automation.CommandCandidate
import com.google.home.automation.Comprehension
import com.google.home.automation.Condition
import com.google.home.automation.Constant
import com.google.home.automation.Constraint
import com.google.home.automation.EventCandidate
import com.google.home.automation.ExpressionWithId
import com.google.home.automation.FieldSelect
import com.google.home.automation.ListContains
import com.google.home.automation.ListGet
import com.google.home.automation.ListIn
import com.google.home.automation.ListSize
import com.google.home.automation.ManualStarter
import com.google.home.automation.MissingStructureAddressSetup
import com.google.home.automation.Node
import com.google.home.automation.NumberRangeConstraint
import com.google.home.automation.ParallelFlow
import com.google.home.automation.Reference
import com.google.home.automation.SelectFlow
import com.google.home.automation.SequentialFlow
import com.google.home.automation.Starter
import com.google.home.automation.StateReader
import com.google.home.automation.TernaryExpression
import com.google.home.automation.TraitAttributesCandidate
import com.google.home.automation.UnaryExpression
import com.google.home.automation.UnknownExpression
import com.google.home.automation.automation
import com.google.home.automation.equals
import com.google.home.automation.lessThan
import com.google.home.automation.sequential
import com.google.home.google.ExtendedColorControl
import com.google.home.google.LightEffects
import com.google.home.google.Notification
import com.google.home.google.SimplifiedThermostat
import com.google.home.google.SimplifiedThermostatTrait
import com.google.home.google.Time
import com.google.home.matter.standard.BasicInformation
import com.google.home.matter.standard.ColorControl
import com.google.home.matter.standard.ColorTemperatureLightDevice
import com.google.home.matter.standard.DimmableLightDevice
import com.google.home.matter.standard.DoorLock
import com.google.home.matter.standard.DoorLockDevice
import com.google.home.matter.standard.ExtendedColorLightDevice
import com.google.home.matter.standard.LevelControl
import com.google.home.matter.standard.LevelControlTrait
import com.google.home.matter.standard.OccupancySensing
import com.google.home.matter.standard.OccupancySensing.Companion.occupancy
import com.google.home.matter.standard.OccupancySensingTrait
import com.google.home.matter.standard.OccupancySensorDevice
import com.google.home.matter.standard.OnOff
import com.google.home.matter.standard.OnOff.Companion.onOff
import com.google.home.matter.standard.OnOffLightDevice
import com.google.home.matter.standard.OnOffTrait
import com.google.home.matter.standard.RoomAirConditionerDevice
import com.google.home.matter.standard.TemperatureMeasurement
import com.google.home.matter.standard.TemperatureMeasurement.Companion.measuredValue
import com.google.home.matter.standard.TemperatureSensorDevice
import com.google.home.matter.standard.Thermostat
import com.google.home.matter.standard.Thermostat.Companion.localTemperature
import com.google.home.matter.standard.ThermostatDevice
import com.google.home.matter.standard.WindowCovering
import com.google.home.matter.standard.WindowCoveringDevice
import com.google.home.matter.standard.WindowCoveringTrait
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.launch

private const val TAG = "SampleApp:Tester"

suspend fun Structure.createAutomationWithDebug(draftAutomation: BaseAutomation): Automation? {
    // Create the automation in the structure
    Log.d(TAG, "createAutomationWithDebug: ${draftAutomation.name} Start")
    val automation = this.createAutomation(draftAutomation)
    Log.d(TAG, "createAutomationWithDebug: ${automation.name} created")
    if (automation.isValid) {
        // Note: .execute() only works when ManualStarter is enabled.
        // Otherwise it will have execution error:
        // error com.google.home.HomeException: 9: Automation not manually executable.
        // automation.execute()
        Log.d(TAG, "createAutomationWithDebug: ${draftAutomation.name} is valid!!")
    }
    if (!automation.isActive) {
        Log.w(
            TAG, "createAutomationWithDebug: ${draftAutomation.name}: automation not active, " +
          "isValid=${automation.isValid}" +
          "${automation.validationIssues}")
    }
    Log.d(TAG, "createAutomationWithDebug: ${draftAutomation.name} Done")
    return automation
}

@Suppress("UNUSED_VARIABLE", "UNUSED_PARAMETER", "unused", "ConstantConditionIf")
class Tester private constructor(private val homeClientRef: HomeClient) : ViewModel() {

    private val homeClient: HomeClient = homeClientRef

    companion object {
        @Volatile
        private var INSTANCE: Tester? = null
        private val initialized = AtomicBoolean(false)

        fun getInstance(homeClientRef: HomeClient): Tester {
            if (initialized.getAndSet(true)) {
                return INSTANCE!!
            }
            synchronized(this) {
                if (INSTANCE == null) {
                    INSTANCE = Tester(homeClientRef)
                }
                return INSTANCE!!
            }
        }
    }

    fun HomeDevice.getParentId() : String? {
        val pattern = "parentDeviceId=Id\\(id=(.*?)\\)".toRegex()
        val matchResult = pattern.find(this.toString()) // Replace with the string you want to search in
        if (matchResult != null) {
            val parentDeviceId = matchResult.groupValues[1]
            Log.d(TAG, "Variable found: $parentDeviceId")
            return parentDeviceId
        } else {
            Log.d(TAG, "No match found.")
        }
        return null
    }

    data class DeviceInfo(
        val name: String,
        val id: String,
        val traitCount: Int = 0,
        val componentCount: Int = 0,
        val matterEndpointCount: Int = 0,
        val cloudEndpointCount: Int = 0,
    ) {
        override fun toString(): String {
            return "DeviceInfo(name='$name', id='$id', traitCount=$traitCount, componentCount=$componentCount, matterEndpointCount=$matterEndpointCount, cloudEndpointCount=$cloudEndpointCount)"
        }
    }

    /**
     * Returns a flow of the trait of the given type from the given device type.
     *
     * @param trait The trait factory to get the trait for
     * @param type The type to get the trait from
     */
    fun <T : Trait> HomeDevice.traitFromType(
        trait: TraitFactory<T>,
        type: DeviceTypeFactory<out DeviceType>,
    ): Flow<T> =
        this.type(type)
            .transform { typeInstance -> typeInstance.trait(trait)?.let { emit(it) } }
            .distinctUntilChanged()

    /** Returns a flow containing a list of deviceTypes */
    fun HomeDevice.getDeviceTypes(scope: CoroutineScope): StateFlow<List<DeviceType>> {
        return types()
            .map { deviceSet -> deviceSet.toList() }
            .stateIn(scope = scope, started = SharingStarted.WhileSubscribed(), emptyList())
    }
    fun HomeDevice.getDeviceInfo() =
        DeviceInfo(
            name,
            id.id,
            supportedTraits.count { has(it) }
        )

    fun HomeDevice.isChildDeviceOf(parentDevice: HomeDevice) : Boolean {
        val parentDeviceId = parentDevice.getDeviceInfo().id
        val searchKey = "parentDeviceId=Id(id=$parentDeviceId)"
        val isChild = this.toString().contains(searchKey, ignoreCase = true)
        // Log.d(TAG, "${this.name} is Child of ${parentDevice.name}? $isChild")
        return isChild
    }
    fun buildDeviceTree(homeDeviceSet :  Set<HomeDevice>) {
        Log.d(TAG, "Build Device Tree In")
        val parentDeviceList = homeDeviceSet.filter {device ->
            device.isChildDeviceOf(device)
        }
        homeDeviceSet.forEach { device ->
            parentDeviceList.forEach { parentDevice ->
                if ((device.getDeviceInfo().id != parentDevice.getDeviceInfo().id) &&
                    (device.isChildDeviceOf(parentDevice))) {
                    Log.d(TAG, "Device ${device.name} is a child of ${parentDevice.name}")
                }
            }
        }
        Log.d(TAG, "Build Device Tree Out")
    }
    // ============================================================================================
    // Automation API
    // ============================================================================================
    private suspend fun createTestAutomation(
        automationName: String,
    ) {
        val structure = homeClient.structures().list().first()
        val deviceId1 = "device@ee9b4ca5-ecc8-4249-a872-2bef7d1a18a1"
        val deviceId2 = "device@ee9b4ca5-ecc8-4249-a872-2bef7d1a18a1"
        var testAutomation =
            getAutomationById(structure, "automation@54a6900d-4299-4907-8716-9502c2613cd6")
                ?: getAutomationByName(structure, automationName)
        // Note: device ID is not Name
        val device: HomeDevice? =
            homeClient.devices().get(Id(deviceId1))
        Log.w(TAG, "device is $device")
        val device1: HomeDevice? =
            homeClient.devices().get(Id(deviceId1))
        val device2: HomeDevice? =
            homeClient.devices().get(Id(deviceId2))
        if ((device1 != null) && (device2 != null)) {
            val draftAutomation = automation {
                name = automationName
                description = "Turn on a device when another device is turned on."
                sequential {
                    val starterNode = starter<_>(device1, OnOffLightDevice, trait = OnOff)
                    condition() { expression = starterNode.onOff equals true }
                    action(device2, OnOffLightDevice) { command(OnOff.on()) }
                }
            }
            try {
                // Create the automation in the structure
                val automation = structure.createAutomationWithDebug(draftAutomation)
            } catch (e: HomeException) {
                Log.w(TAG, "$automationName error: $e")
            }
            val automation2 = automation {
                name = "${automationName}2"
                description = "Turn on a device when another device is turned on."
                sequential {
                    val starterNode =     // TypedExpression<OnOff>
                        starter<_>(         // The <> (angle brackets) likely denote a generic type parameter,
                            // which is not explicitly specified here (using _ as a placeholder).
                            // This suggests that the starter function can work with different
                            // types of devices, traits, and factories.
                            device1,          // HomeDevice
                            OnOffLightDevice, // DeviceTypeFactory<DeviceType>
                            trait = OnOff      // TraitFactory<Trait>
                        )
                    condition() { expression = starterNode.onOff equals true }
                    action(device2, OnOffLightDevice) { command(OnOff.on()) }
                }
            }
        }
    }
    private suspend fun createSimpleAutomationWithTimerAndLight(
        automationName: String,
        hour: Int,
        min: Int,
        structureName: String,
        homeName: String?,
        deviceName: String?,
    ) {
        val structure = homeClient.structures().list().firstOrNull {
            // if name is null, choose first one
            homeName == null || it.name == homeName
        }.takeIf {
            it != null
        } ?: run {
            Log.w(TAG, "Not found Home name: $homeName")
            return
        }
        val lightDevice = structure.devices().list().firstOrNull {
            (
              // Must be one type of light
              it.has(OnOffLightDevice) ||
                it.has(DimmableLightDevice) ||
                it.has(ColorTemperatureLightDevice) ||
                it.has (ExtendedColorLightDevice)
              // it.has(Brightness)
              ) && (
              // if name is null, choose first one
              deviceName == null || it.name == deviceName
              )
        }.takeIf {
            it != null
        } ?: run {
            Log.w(TAG, "$automationName Not found Home device: $deviceName")
            return
        }
        val draftAutomation = automation {
            name = automationName
            description = "Toggle a light when time is up."
            sequential {
                // starter
                val starter = starter(structure, Time.ScheduledTimeEvent) {
                    parameter(
                        Time.ScheduledTimeEvent.clockTime(
                            LocalTime.of(hour, min, 0, 0)
                        )
                    )
                }
                // action
                action(lightDevice, OnOffLightDevice) {
                    command(
                        OnOff.toggle()
                    )
                }
            }
        }
        try {
            // Create the automation in the structure
            val automation = structure.createAutomationWithDebug(draftAutomation)
        } catch (e: HomeException) {
            Log.w(TAG, "$automationName error: $e")
        }
    }
    // TODO: test this
    private suspend fun createSimpleAutomationTimerLight(
        automationName: String,
        structureName: String,
        scheduledTimeInSecond: Int,
    ): String {
        // get the Structure
        val structure = homeClient.structures().list().firstOrNull {
            it.name == structureName
        } // No such function: .findStructureByName(structureName)
        if (structure == null) {
            Log.w(TAG, "Unable to find structure $structureName")
            return null.toString()
        }
        // get an event candidate
        val clockTimeStarter =
            structure
                .allCandidates()                  // Flow<Set<NodeCandidate>>
                .first()                          // Set<NodeCandidate>
                .firstOrNull { candidate ->       // NodeCandidate
                    candidate is EventCandidate &&
                      candidate.eventFactory == Time.ScheduledTimeEvent
                } as EventCandidate?
        // retrieve the first 'DownOrClose' command encountered
        val downOrCloseCommand =
            structure.allCandidates().first().firstOrNull {
                    candidate ->
                candidate is CommandCandidate
                  && candidate.commandDescriptor == WindowCoveringTrait.DownOrCloseCommand
            } as CommandCandidate?
        // Find a device supporting WindowCoveringDevice
        val blinds: HomeDevice? = structure.devices().list().firstOrNull { device ->
            device.has(WindowCoveringDevice)
        }
        // prompt user to select the WindowCoveringDevice
        //...
        if (clockTimeStarter !=null && downOrCloseCommand !=null && blinds != null) {
            // Create the draft automation
            val draftAutomation = automation {
                name = automationName
                description = "Timer Automation with Candidate "
                isActive = true
                sequential {
                    // starter
                    val mainStarter = starter<_>(structure, Time.ScheduledTimeEvent) {
                        parameter(          // Parameter.parameter()
                            Time.ScheduledTimeEvent.clockTime(
                                // Note: set the event time (in seconds) of a day
                                LocalTime.ofSecondOfDay(  // Obtains an instance of LocalTime from a second-of-day value.
                                    scheduledTimeInSecond.toLong()
                                )
                            )
                        )
                    }
                    // action
                    action(blinds, WindowCoveringDevice) {
                        command(            // ActionBuilder.command()
                            WindowCovering    // companion object of WindowCovering : TraitFactory<WindowCovering
                                .downOrClose()  // com.google.home.automation.Command
                        )
                    }
                }
            }
            try {
                // Create the automation in the structure
                val automation = structure.createAutomationWithDebug(draftAutomation)
                return automation?.id?.id ?: null.toString()
            } catch (e: HomeException) {
                Log.w(TAG, "$automationName error: $e")
            }
        } else  {
            Log.w(TAG, "$automationName no candidate:")
        }
        return null.toString()
    }
    private suspend fun createSimpleAutomationLightOnMoveToLevel(
        automationName: String,
        structureName: String,
    ): String {
        // get the Structure
        val structure = homeClient.structures().list().firstOrNull {
            it.name == structureName
        } ?: run {
            Log.w(TAG, "Unable to find structure $structureName")
            return null.toString()
        }
        /**
         * When I turn on the light, move the brightness level to 55
         */
        val allCandidates = structure.allCandidates().first()
        val dimmableLightDevice = structure.devices()
            .list()
            .first {
                it.has(OnOff) &&
                  it.has(LevelControl)
            }
        //  TraitAttributesCandidate ( OnOff Trait)
        val starterCandidate =
            allCandidates
                .filterIsInstance<TraitAttributesCandidate>()
                .firstOrNull {
                    it.entity == dimmableLightDevice &&
                      it.trait == OnOff
                }
        // CommandCandidate
        val actionCandidate =
            allCandidates
                .filterIsInstance<CommandCandidate>()
                .firstOrNull {
                    it.entity == dimmableLightDevice &&
                      it.commandDescriptor == LevelControlTrait.MoveToLevelCommand
                }
        if (starterCandidate != null && actionCandidate != null) {
            // Create the draft automation
            val draftAutomation = automation {
                name = automationName
                description = "When I turn on the light, move the brightness level to 55"
                isActive = true
                sequential {
                    val starter =  starter<_>(dimmableLightDevice, OnOffLightDevice, OnOff)
                    condition { expression = starter.onOff equals true }
                    action(dimmableLightDevice, DimmableLightDevice) {
                        mapOf(
                            LevelControlTrait
                                .MoveToLevelCommand
                                .Request
                                .CommandFields
                                .level to 55u.toUByte()
                        )
                    }
                }
            }
            try {
                // Create the automation in the structure
                val automation = structure.createAutomationWithDebug(draftAutomation)
                return automation?.id?.id ?: null.toString()
            } catch (e: HomeException) {
                Log.w(TAG, "$automationName error: $e")
            }
        } else {
            Log.w(
                TAG, "$automationName no candidate:" +
              " starterCandidate=$starterCandidate" +
              ", actionCandidate$starterCandidate")
        }
        return null.toString()
    }
    private suspend fun createSimpleAutomationOccupancySensorToggleLight(
        automationName: String,
        structureName: String,
        occupancySensorDeviceName: String?,
        lightDeviceName: String?,
    ): String {
        // get the Structure
        val structure = homeClient.structures().list().firstOrNull {
            it.name == structureName
        } // No such function: .findStructureByName(structureName)
        if (structure == null) {
            Log.w(TAG, "$automationName: Unable to find structure $structureName")
            return null.toString()
        }
        /**
         * When I occupancy sensor detects occupied, turn on the light
         */
        val allCandidates = structure.allCandidates().first()
        val occupancySensorDevice = structure.devices() // HomeObjectFlow<Device>
            .list()         // Set<Device>
            .firstOrNull {  // firstOrNull + filter
                (
                  occupancySensorDeviceName == null || it.name == occupancySensorDeviceName) &&
                  it.has(OccupancySensorDevice)
            }.takeIf {    // lightDevice
                it != null
            }             // lightDevice
            ?: run {
                Log.w(TAG, "$automationName: Unable to find occupancy sensor device $occupancySensorDeviceName")
                return null.toString()
            }
        //  TraitAttributesCandidate ( MotionDetection Trait)
        val starterCandidate =
            allCandidates
                .filterIsInstance<TraitAttributesCandidate>()
                .firstOrNull {
                    it.entity == occupancySensorDevice &&
                      it.trait == OccupancySensing
                }
        val lightDevice = structure.devices() // HomeObjectFlow<Device>
            .list()         // Set<Device>
            .firstOrNull {  // firstOrNull + filter
                (
                  lightDeviceName == null || it.name == lightDeviceName) &&
                  it.has(OnOffLightDevice)
            }.takeIf {    // lightDevice
                it != null
            }             // lightDevice
            ?: run {
                Log.w(TAG, "$automationName: Unable to find light device $lightDeviceName")
                return null.toString()
            }
        // CommandCandidate
        val actionCandidate =
            allCandidates
                .filterIsInstance<CommandCandidate>()
                .firstOrNull {
                    it.entity == lightDevice &&
                      it.commandDescriptor == OnOffTrait.OnCommand
                }
        Log.d(TAG, "$automationName: creating automation with ${occupancySensorDevice.name} and ${lightDevice.name}")
        if (starterCandidate != null && actionCandidate != null) {
            // Create the draft automation
            val draftAutomation = automation {
                name = automationName
                description = "When I occupancy sensor detects occupied, turn on the light"
                isActive = true
                sequential {
                    val starter =  starter<_>(occupancySensorDevice, OccupancySensorDevice, OccupancySensing)
                    // Note: Occupancy is not simple Boolean
                    condition {
                        expression = starter.occupancy equals
                          OccupancySensingTrait.OccupancyBitmap(occupied = true)
                    }
                    // ignore the starter for one second after it was last triggered
                    /* Note: Suppress for 1 second will get invalid : SUPPRESSION_DURATION_OUT_OF_RANGE
                    suppressFor(java.time.Duration.ofSeconds(1))
                     */
                    action(lightDevice, OnOffLightDevice) {
                        // Note: OnCommand doesn't need mapOf since its request doesn't have arguments ?
                        command(OnOff.on())
                        /*
                        OnOffTrait
                          .OnCommand
                          .Request()
                         */
                    }
                }
            }
            try {
                // Create the automation in the structure
                val automation = structure.createAutomationWithDebug(draftAutomation)
                return automation?.id?.id ?: null.toString()
            } catch (e: HomeException) {
                Log.w(TAG, "$automationName: error $e")
            }
        } else {
            Log.w(TAG, "$automationName: no candidate starterCandidate=$starterCandidate")
            Log.w(TAG, "$automationName: no candidate actionCandidate=$actionCandidate")
        }
        return null.toString()
    }
    private suspend fun createSimpleAutomationLightAndToggleLight(
        automationName: String,
        structureName: String,
        lightDeviceName1: String?,
        lightDeviceName2: String?,
    ): String {
        // get the Structure
        val structure = homeClient.structures().list().firstOrNull {
            it.name == structureName
        } // No such function: .findStructureByName(structureName)
        if (structure == null) {
            Log.w(TAG, "$automationName: Unable to find structure $structureName")
            return null.toString()
        }
        val allCandidates = structure.allCandidates().first()
        val lightDevice1 = structure.devices() // HomeObjectFlow<Device>
            .list()         // Set<Device>
            .firstOrNull {  // firstOrNull + filter
                (
                  lightDeviceName1 == null || it.name == lightDeviceName1) &&
                  it.has(OnOffLightDevice)
            }.takeIf {    // lightDevice
                it != null
            }             // lightDevice
            ?: run {
                Log.w(TAG, "$automationName: Unable to find lightDeviceName1 $lightDeviceName1")
                return null.toString()
            }
        //  TraitAttributesCandidate ( MotionDetection Trait)
        val starterCandidate =
            allCandidates
                .filterIsInstance<TraitAttributesCandidate>()
                .firstOrNull {
                    it.entity == lightDevice1 &&
                      it.trait == OnOff
                }
        val lightDevice2 = structure.devices() // HomeObjectFlow<Device>
            .list()         // Set<Device>
            .firstOrNull {  // firstOrNull + filter
                (
                  lightDeviceName2 == null || it.name == lightDeviceName2) &&
                  it.has(OnOffLightDevice)
            }.takeIf {    // lightDevice
                it != null
            }             // lightDevice
            ?: run {
                Log.w(TAG, "$automationName: Unable to find lightDeviceName2 $lightDeviceName2")
                return null.toString()
            }
        // CommandCandidate
        val actionCandidate =
            allCandidates
                .filterIsInstance<CommandCandidate>()
                .firstOrNull {
                    it.entity == lightDevice2 &&
                      it.commandDescriptor == OnOffTrait.OnCommand
                }
        Log.d(TAG, "$automationName: creating automation with ${lightDevice1.name} and ${lightDevice2.name}")
        if (starterCandidate != null && actionCandidate != null) {
            // Create the draft automation
            val draftAutomation = automation {
                name = automationName
                description = "When I turn on a light, the other light is also turned on"
                isActive = true
                sequential {
                    // With manual starter, so we need select here
                    select {
                        sequential {
                            val starter =  starter<_>(lightDevice1, OnOffLightDevice, OnOff)
                            // Note: Occupancy is not simple Boolean
                            condition {
                                expression = starter.onOff equals true
                            }
                        }
                        manualStarter()
                    }
                    parallel {
                        action(lightDevice2, OnOffLightDevice) {
                            command(OnOff.toggle())
                        }
                    }
                }
            }
            try {
                // Create the automation in the structure
                val automation = structure.createAutomationWithDebug(draftAutomation)
                return automation?.id?.id ?: null.toString()
            } catch (e: HomeException) {
                Log.w(TAG, "$automationName error: $e")
            }
        } else {
            Log.w(TAG, "$automationName: no candidate starterCandidate=$starterCandidate")
            Log.w(TAG, "$automationName: no candidate actionCandidate=$actionCandidate")
        }
        return null.toString()
    }
    private suspend fun createSimpleAutomationLightAndToggleLight2(
        automationName: String,
        structureName: String,
        lightDeviceName1: String?,
        lightDeviceName2: String?,
    ): String {
        // get the Structure
        val structure = homeClient.structures().list().firstOrNull {
            it.name == structureName
        } ?: run {
            Log.w(TAG, "$automationName: Unable to find structure $structureName")
            return null.toString()
        }
        val lightDevice1 = structure.devices() // HomeObjectFlow<Device>
            .list()         // Set<Device>
            .firstOrNull {  // firstOrNull + filter
                (
                  lightDeviceName1 == null || it.name == lightDeviceName1) &&
                  it.has(OnOffLightDevice)
            } ?: run {
            Log.w(TAG, "$automationName: Unable to find lightDeviceName1 $lightDeviceName1")
            return null.toString()
        }
        val lightDevice2 = structure.devices() // HomeObjectFlow<Device>
            .list()         // Set<Device>
            .firstOrNull {  // firstOrNull + filter
                (
                  lightDeviceName2 == null || it.name == lightDeviceName2) &&
                  it.has(OnOffLightDevice)
            } ?: run {
            Log.w(TAG, "$automationName: Unable to find lightDeviceName2 $lightDeviceName2")
            return null.toString()
        }
        Log.d(TAG, "$automationName: creating automation with ${lightDevice1.name} and ${lightDevice2.name}")
        // Create the draft automation
        val draftAutomation = automation {
            name = automationName
            description = "When I turn on a light, the other light is also turned on"
            isActive = true
            sequential {
                // With manual starter, so we need select here
                select {
                    sequential {
                        val starter =  starter<_>(lightDevice1, OnOffLightDevice, OnOff)
                        // Note: Occupancy is not simple Boolean
                        condition {
                            expression = starter.onOff equals true
                        }
                    }
                    manualStarter()
                }
                parallel {
                    action(lightDevice2, OnOffLightDevice) {
                        command(OnOff.on())
                    }
                }
            }
        }
        try {
            // Create the automation in the structure
            val automation = structure.createAutomationWithDebug(draftAutomation)
            return automation?.id?.id ?: null.toString()
        } catch (e: HomeException) {
            Log.w(TAG, "$automationName error: $e")
        }
        return null.toString()
    }
    private suspend fun createSimpleAutomationLightAndThermostatWithSimplifiedTrait(
        automationName: String,
        structureName: String,
        lightDeviceName1: String?,
        thermostatDeviceName: String?,
    ): String {
        // get the Structure
        val structure = homeClient.structures().list().firstOrNull {
            it.name == structureName
        } ?: run {
            Log.w(TAG, "$automationName: Unable to find structure $structureName")
            return null.toString()
        }
        val lightDevice1 = structure.devices() // HomeObjectFlow<Device>
            .list()         // Set<Device>
            .firstOrNull {  // firstOrNull + filter
                (
                  lightDeviceName1 == null || it.name == lightDeviceName1) &&
                  it.has(OnOffLightDevice)
            } ?: run {
            Log.w(TAG, "$automationName: Unable to find lightDeviceName1 $lightDeviceName1")
            return null.toString()
        }
        val thermostatDevice = structure.devices() // HomeObjectFlow<Device>
            .list()         // Set<Device>
            .firstOrNull {  // firstOrNull + filter
                (
                  thermostatDeviceName == null || it.name == thermostatDeviceName) &&
                  it.has(ThermostatDevice)
            } ?: run {
            Log.w(TAG, "$automationName: Unable to find thermostatDeviceName $thermostatDeviceName")
            return null.toString()
        }
        Log.d(TAG, "$automationName: creating automation with ${lightDevice1.name} and ${thermostatDevice.name}")
        val thermostatTrait = thermostatDevice.traitFromType(Thermostat, ThermostatDevice).first()
        Log.d(TAG, "$automationName: ${thermostatDevice.name} supports ${thermostatTrait.featureMap}")
        // Create the draft automation
        val draftAutomation = automation {
            name = automationName
            description = "When turn on the light, use SimplifiedThermostatTrait to set Thermostat to Auto"
            isActive = true
            sequential {
                // With manual starter, so we need select here
                select {
                    sequential {
                        val starter =  starter<_>(lightDevice1, OnOffLightDevice, OnOff)
                        // Note: Occupancy is not simple Boolean
                        condition {
                            expression = starter.onOff equals true
                        }
                    }
                    manualStarter()
                }
                parallel {
                    // Note: The 2 actions below actually conflict. Please enable one only
                    // =============================================================
                    action(thermostatDevice, ThermostatDevice) {
                        // Use SimplifiedThermostatTrait to set
                        command(SimplifiedThermostat.setSystemMode(SimplifiedThermostatTrait.SystemModeEnum.Auto))
                    }
                    // TODO: This seems doesn't work
                    // action(thermostatDevice, ThermostatDevice) {
                    //   // Use ThermostatTrait to set SystemMode
                    //   update(Thermostat) {
                    //     setSystemMode(ThermostatTrait.SystemModeEnum.Off)
                    //     setOccupiedHeatingSetpoint(15)
                    //   }
                    // }
                }
            }
        }
        try {
            // Create the automation in the structure
            val automation = structure.createAutomationWithDebug(draftAutomation)
            return automation?.id?.id ?: null.toString()
        } catch (e: HomeException) {
            Log.w(TAG, "$automationName error: $e")
        }
        return null.toString()
    }
    // TODO: 1. Automation starter by Thermostat localTemperatue seems to not working
    // TODO: 2. controlling blinds seems not working with Google Home Playground
    private suspend fun createSimpleAutomationAllThermostatThermalSensorAndAllBlinds(
        automationName: String,
        structureName: String,
    ): String {
        // get the Structure
        val structure = homeClient.structures().list().firstOrNull {
            it.name == structureName
        } ?: run {
            Log.w(TAG, "$automationName: Unable to find structure $structureName")
            return null.toString()
        }
        val allCandidates = structure.allCandidates().first()
        val starterCandidates =
            allCandidates
                .filterIsInstance<TraitAttributesCandidate>()
                .filter { traitAttributeCandidate ->
                    if (traitAttributeCandidate.unsupportedReasons.isNotEmpty()) {
                        Log.d(TAG, "$automationName: skip unsupported traitAttributeCandidate, reason ${traitAttributeCandidate.unsupportedReasons}")
                    }
                    traitAttributeCandidate.unsupportedReasons.isEmpty() &&
                      (traitAttributeCandidate.trait == TemperatureMeasurement ||
                        traitAttributeCandidate.trait == Thermostat)
                }
        val actionCandidates =
            allCandidates
                .filterIsInstance<CommandCandidate>()
                .filter { commandCandidate ->
                    if (commandCandidate.unsupportedReasons.isNotEmpty()) {
                        Log.d(TAG, "$automationName: skip unsupported commandCandidate, reason ${commandCandidate.unsupportedReasons}")
                    }
                    // Notes: Adding OnOff Light since WindowCovering traits is not compatible with Google Playground
                    // Notes: WindowCoveringTrait.DownOrCloseCommand is the key to filter out the only candidate
                    commandCandidate.unsupportedReasons.isEmpty() &&
                      ( (commandCandidate.trait == WindowCovering &&
                        commandCandidate.commandDescriptor == WindowCoveringTrait.DownOrCloseCommand ) ||
                        (commandCandidate.trait == OnOff &&
                          commandCandidate.types.firstOrNull{it == OnOffLightDevice} != null &&
                          commandCandidate.commandDescriptor == OnOffTrait.OffCommand))
                }
        if (starterCandidates.isEmpty() || actionCandidates.isEmpty()) {
            Log.w(TAG, "$automationName: no candidate starterCandidates=$starterCandidates")
            Log.w(TAG, "$automationName: no candidate actionCandidates=$actionCandidates")
            return null.toString()
        }
        // Needing to map device by id
        val devicesMap: MutableMap<Id, HomeDevice> = emptyMap<Id, HomeDevice>().toMutableMap()
        for (starterCandidate in starterCandidates) {
            devicesMap[starterCandidate.entity.id] = structure.devices().get(starterCandidate.entity.id)!!
        }
        for (actionCandidate in actionCandidates) {
            devicesMap[actionCandidate.entity.id] = structure.devices().get(actionCandidate.entity.id)!!
        }
        // Create the draft automation
        val draftAutomation = automation {
            name = automationName
            description = "When any device measures temperature is below 20 degrees, close all blinds"
            isActive = true
            sequential {
                select {  // Multiple Starters
                    for (starterCandidate in starterCandidates) {
                        Log.d(TAG, "$automationName: starterCandidate=$starterCandidate")
                        val device = devicesMap[starterCandidate.entity.id]!!
                        if ((starterCandidate.trait == Thermostat) && (device.has(ThermostatDevice))) {
                            sequential {
                                val starter = starter<_>(device, ThermostatDevice, Thermostat)
                                condition { expression = starter.localTemperature lessThan 20 }
                            }
                        } else if ((starterCandidate.trait == Thermostat) && (device.has(RoomAirConditionerDevice))) {
                            sequential {
                                val starter = starter<_>(device, RoomAirConditionerDevice, Thermostat)
                                condition { expression = starter.localTemperature lessThan 20 }
                            }
                        } else if ((starterCandidate.trait == TemperatureMeasurement) && (device.has(TemperatureSensorDevice))) {
                            sequential {
                                val starter = starter<_>(device, TemperatureSensorDevice, TemperatureMeasurement)
                                condition { expression = starter.measuredValue lessThan 20 }
                            }
                        } else {
                            Log.w(TAG, "$automationName: Unknown starterCandidate device need to be supported, $device")
                        }
                    }
                    // Note: Not sure if starter works, use a timer to check the action
                    if (true) {
                        val now = LocalDateTime.now().plusSeconds(15)
                        val starter = starter(structure, Time.ScheduledTimeEvent) {
                            parameter(
                                Time.ScheduledTimeEvent.clockTime(
                                    LocalTime.of(now.hour, now.minute, now.second, 0)
                                )
                            )
                        }
                    }
                    manualStarter()
                }
                parallel {
                    for (actionCandidate in actionCandidates) {
                        Log.d(TAG, "$automationName: actionCandidate=$actionCandidate")
                        val device = devicesMap[actionCandidate.entity.id]!!
                        // Iterate all blind devices to open
                        if (actionCandidate.trait == WindowCovering) {
                            action(device, WindowCoveringDevice) { command(WindowCovering.downOrClose()) }
                        } else if (actionCandidate.trait == OnOff) {
                            action(device, OnOffLightDevice) { command(OnOff.off()) }
                        } else {
                            Log.w(TAG, "$automationName: Unknown actionCandidate device need to be supported, $device")
                        }
                    }
                }
            }
        }
        try {
            // Create the automation in the structure
            val automation = structure.createAutomationWithDebug(draftAutomation)
            return automation?.id?.id ?: null.toString()
        } catch (e: HomeException) {
            Log.w(TAG, "$automationName error: $e")
        }
        return null.toString()
    }
    // TODO: test this.
    private suspend fun checkForPrerequisites() {
        val homeManager = homeClient
        val structure = homeManager.structures().list().single()
        val allCandidates = structure
            .allCandidates()      // Flow<Set<NodeCandidate>>
            .firstOrNull()        // Set<NodeCandidate>
        val scheduledStarterCandidate = allCandidates?.first {
            it is EventCandidate &&
              it.eventFactory == Time.ScheduledTimeEvent
        }
        if (scheduledStarterCandidate
                ?.unsupportedReasons?.any{
                    it is MissingStructureAddressSetup
                } == true
        ) {
            // Toast("No Structure Address setup. Redirecting to GHA to set up an address.")
            // launchChangeAddress(...)
        }
    }
    private suspend fun getAutomationById (structure: Structure, id: String) : Automation? {
        val automationFound = structure
            .automations()                                          // HomeObjectsFlow<Automation>
            .map { automationSet ->                                 // Set<Automation>
                automationSet.firstOrNull { automation ->             // Automation
                    Log.d(TAG, "automation is ${automation.id}")
                    automation.id == Id(id)
                }
            }                                                       // Flow<Automation>
            .firstOrNull()                                          // Automation?
        Log.d(TAG, "getAutomationById found ${automationFound?.id ?: "null"}")
        return automationFound
    }
    private suspend fun getAutomationByName(structure: Structure, name: String) : Automation? {
        Log.d(TAG, "getAutomationByName is name $name}")
        val automationFound = structure.automations().map {
            it.firstOrNull { automation ->
                automation.name == name
            }
        }.firstOrNull()
        Log.d(TAG, "getAutomationByName is ${automationFound?.name ?: "null"}")
        return automationFound
    }
    private suspend fun getAllAutomationForDevice() {
        fun collectDescendants(node: Node): List<Node> {
            val d: MutableList<Node> = mutableListOf(node)
            val children: List<Node> =
                when (node) {
                    is SequentialFlow -> node.nodes
                    is ParallelFlow -> node.nodes
                    is SelectFlow -> node.nodes
                    else -> emptyList()
                }
            for (c in children) {
                d += collectDescendants(c)
            }
            return d
        }
        // val myDeviceId = "device@452f78ce8-0143-84a-7e32-1d99ab54c83a"
        val myDeviceId = "device@ee9b4ca5-ecc8-4249-a872-2bef7d1a18a1"
        val structure = homeClient.structures().list().single()
        val automations =
            structure.automations().first().filter {
                    automation: Automation ->
                collectDescendants(automation.automationGraph!!).any { node: Node ->
                    when (node) {
                        is Starter -> node.entity.id.id == myDeviceId
                        is StateReader -> node.entity.id.id == myDeviceId
                        is Action -> node.entity.id.id == myDeviceId
                        else -> false
                    }
                }
            }
        Log.d(TAG, "Search by device id $myDeviceId found ${automations.size} automations")
    }
    private suspend fun updateMyAutomation(id:String?, name:String?){
        val structure = homeClient.structures().list().single()
        val automation = structure.automations().map {
            it.firstOrNull { automation ->
                if (id != null)  { automation.id == Id(id) }
                else if (name != null) { automation.name == name }
                else false
            }
        }.firstOrNull()
        if (automation == null) {
            Log.d(TAG, "updateMyAutomation: Not Found")
            return
        }
        try {
            automation.update {
                this.name = "Flashing lights 2"
                this.automationGraph =          // SequentialFlow?
                    sequential {                  // com.google.home.automation.AutomationBuilderKt.sequential
                        // // https://home.devsite.corp.google.com/reference/com/google/home/automation/AutomationBuilderKt?db=home-features&hl=en#sequential(kotlin.Function1)
                        val unused = starter(structure, Time.ScheduledTimeEvent) {
                            parameter(
                                Time                    // class Time : PlatformTrait, TimeTrait. Attributes
                                    .ScheduledTimeEvent   // EventFactory<Time. ScheduledTimeEvent>
                                    .clockTime            // Parameter
                                        (
                                        LocalTime.of(11, 12, 0, 0)
                                    )
                            )
                        } // TypedExpression<Time. ScheduledTimeEvent>
                        /*
                        condition {
                          // ....
                        }
                         */
                        action(structure) {
                            command(
                                Notification  // companion object of Notification : TraitFactory<Notification>
                                    .sendNotifications(
                                        "title",
                                        optionalArgs = {
                                            body = "body"
                                            optInMemberEmails = listOf("dspeuser1@gmail.com")
                                        },
                                    )
                            )
                        }
                    }
            }
            Log.d(TAG, "updateMyAutomation: Done")
        } catch (e: HomeException) {
            Log.e(TAG, "updateMyAutomation: failed $e")
        }
    }
    private suspend fun deleteMyAutomation(id:String?, name:String?){
        val structure = homeClient.structures().list().single()
        val automation = structure.automations()        // HomeObjectsFlow<Automation
            .map { automationSet ->                       // Set<Automation>
                automationSet.firstOrNull { automation ->   // Automation
                    if (id != null)  { automation.id == Id(id) }
                    else if (name != null) { automation.name == name }
                    else false
                }                                           // Automation
            }                                             // Set<Automation>
            .firstOrNull()
        if (automation == null) {
            Log.d(TAG, "deleteMyAutomation: Not Found")
            return
        }
        try {
            structure.deleteAutomation(automation.id)
            Log.d(TAG, "deleteMyAutomation: Done")
        } catch (e: HomeException) {
            Log.e(TAG, "updateMyAutomation: failed $e")
        }
    }
    private suspend fun deleteAllAutomation(structureName: String){
        // get the Structure
        val structure = homeClient.structures().list().firstOrNull {
            it.name == structureName
        } // No such function: .findStructureByName(structureName)
        if (structure == null) {
            Log.w(TAG, "Unable to find structure $structureName")
            return
        }
        val automation = structure.automations()        // HomeObjectsFlow<Automation
            .list()                                       // Set<Automation>
            .forEach { automation ->                      // Automation
                structure.deleteAutomation(automation.id)
            }                                             // Automation
    }
    private suspend fun dumpAutomationCondition(condition: Condition) {
        Log.d(TAG, "====== Dump automationGraph Condition $condition")
        condition.apply {
            Log.d(TAG, "======= Dump automationGraph Condition.nodeId: ${this.nodeId}")
            Log.d(TAG, "======= Dump automationGraph Condition.forDuration: ${this.forDuration}")
        }
        val expression = condition.expression
        Log.d(TAG, "======= Dump automationGraph Condition.expression: ${expression}")
        when (expression) {
            is Constant -> {
                Log.d(TAG, "========= Dump automationGraph Constant: ${expression.constant}")
            }
            is ExpressionWithId -> {
                Log.d(TAG, "========= Dump automationGraph ExpressionWithId: ")
                when (expression) {
                    is BinaryExpression -> {
                    }
                    is Comprehension -> {
                    }
                    is FieldSelect -> {
                    }
                    is ListContains -> {
                    }
                    is ListGet -> {
                    }
                    is ListIn -> {
                    }
                    is ListSize -> {
                    }
                    is TernaryExpression -> {
                    }
                    is UnaryExpression -> {
                    }
                    is UnknownExpression -> {
                    }
                    else -> {
                    }
                }
                Log.d(TAG, "========= Dump automationGraph ExpressionWithId: Done")
            }
            is Reference -> {
                Log.d(TAG, "========= Dump automationGraph Reference: ${expression.reference}")
            }
            else -> {
                Log.d(TAG, "========= Dump automationGraph Condition: Unknown type $expression")
            }
        }
        Log.d(TAG, "======= Dump automationGraph expression: Done")
        //
        Log.d(TAG, "====== Dump automationGraph Condition: Done")
    }
    private suspend fun dumpAutomationNode(node: Node) {
        if (true) Log.d(TAG, "===== Dump automationGraph.node: Start")
        Log.d(TAG, "====== Dump automationGraph.node.nodeId: ${node.nodeId}")
        when (node) {
            is Action -> {
                Log.d(TAG, "====== Dump automationGraph Action $node")
                node.apply {
                    Log.d(TAG, "======= Dump automationGraph Action.nodeId: ${this.nodeId}")
                    Log.d(TAG, "======= Dump automationGraph Action.entity: ${this.entity}")
                    Log.d(TAG, "======= Dump automationGraph Action.deviceType: ${this.deviceType}")
                    Log.d(TAG, "======= Dump automationGraph Action.behavior: ${this.behavior}")
                }
                Log.d(TAG, "====== Dump automationGraph Action: Done")
            }
            is Condition -> {
                dumpAutomationCondition(node)
            }
            is ManualStarter -> {
                Log.d(TAG, "====== Dump automationGraph ManualStarter: $node")
                Log.d(TAG, "======= Dump automationGraph ManualStarter.nodeId: ${node.nodeId}")
                Log.d(TAG, "====== Dump automationGraph ManualStarter: Done")
            }
            is Starter -> {
                node.apply {
                    Log.d(TAG, "====== Dump automationGraph Starter.nodeId: ${this.nodeId}")
                    Log.d(TAG, "====== Dump automationGraph Starter.entity: ${this.entity}")
                    Log.d(TAG, "====== Dump automationGraph Starter.trait: ${this.trait}")
                    Log.d(TAG, "====== Dump automationGraph Starter.deviceType: ${this.deviceType}")
                    Log.d(TAG, "====== Dump automationGraph Starter.event: ${this.event}")
                    Log.d(TAG, "====== Dump automationGraph Starter.parameters: ${this.parameters}")
                    this.parameters.forEach { parameter ->
                        Log.d(TAG, "====== Dump automationGraph Starter.parameter: ${parameter}")
                        parameter.apply {
                            Log.d(TAG, "====== Dump automationGraph Starter.param: ${this.param}")
                            Log.d(TAG, "====== Dump automationGraph Starter.param.first: ${this.param.first}")
                            Log.d(TAG, "====== Dump automationGraph Starter.param.first.tag: ${this.param.first.tag}")
                            Log.d(TAG, "====== Dump automationGraph Starter.param.first.typeEnum: ${this.param.first.typeEnum}")
                            Log.d(TAG, "====== Dump automationGraph Starter.param.first.typeName: ${this.param.first.typeName}")
                            Log.d(TAG, "====== Dump automationGraph Starter.param.first.descriptor: ${this.param.first.descriptor}")
                            Log.d(TAG, "====== Dump automationGraph Starter.param.second: ${this.param.second}")
                        }
                    }
                    Log.d(TAG, "====== Dump automationGraph Starter.traitId: ${this.traitId}")
                    Log.d(TAG, "====== Dump automationGraph Starter.output: ${this.output}")
                }
            }
            is StateReader -> {
                node.apply {
                    Log.d(TAG, "====== Dump automationGraph StateReader.nodeId: ${this.nodeId}")
                    Log.d(TAG, "====== Dump automationGraph StateReader.entity: ${this.entity}")
                    Log.d(TAG, "====== Dump automationGraph StateReader.trait: ${this.trait}")
                    Log.d(TAG, "====== Dump automationGraph StateReader.deviceType: ${this.deviceType}")
                    Log.d(TAG, "====== Dump automationGraph StateReader.traitId: ${this.traitId}")
                    Log.d(TAG, "====== Dump automationGraph StateReader.output: ${this.output}")
                }
            }
            is ParallelFlow -> {
                dumpAutomationParallelFlow(node)
            }
            is SelectFlow -> {
                dumpAutomationSelectFlow(node)
            }
            is SequentialFlow -> {
                dumpAutomationSequentialFlow(node)
            }
            else -> {
                Log.d(TAG, "====== Dump automation.automationGraph.node: Unknown type: $node")
            }
        }
        Log.d(TAG, "====== Dump automationGraph.node.nodeId: Done")
        Log.d(TAG, "===== Dump automationGraph node: Done")
    }
    private suspend fun dumpAutomationParallelFlow(parallelFlow: ParallelFlow) {
        Log.d(TAG, "===== Dump automationGraph SequentialFlow child nodes: ${parallelFlow}")
        parallelFlow.nodes.forEach { node ->
            dumpAutomationNode(node)
        }
        Log.d(TAG, "===== Dump automationGraph SequentialFlow child nodes: Done")
    }
    private suspend fun dumpAutomationSelectFlow(selectFlow: SelectFlow) {
        Log.d(TAG, "===== Dump automationGraph child SelectFlow child nodes: ${selectFlow}")
        selectFlow.nodes.forEach { node ->
            dumpAutomationNode(node)
        }
        Log.d(TAG, "===== Dump automationGraph child SelectFlow child nodes: Done")
    }
    private suspend fun dumpAutomationSequentialFlow(sequentialFlow: SequentialFlow) {
        Log.d(TAG, "===== Dump automationGraph SequentialFlow child nodes: ${sequentialFlow}")
        sequentialFlow.nodes.forEach { node ->
            dumpAutomationNode(node)
        }
        Log.d(TAG, "===== Dump automationGraph SequentialFlow child nodes: Done")
    }
    private suspend fun dumpAutomation(automation: Automation) {
        //
        Log.i(TAG, "=== Dump automation: $automation")
        automation.apply {
            Log.d(TAG, "==== Dump automation.name: ${this.name}")
            Log.d(TAG, "==== Dump automation.id: ${this.id}")
            Log.d(TAG, "==== Dump automation.description: ${this.description}")
            Log.d(TAG, "==== Dump automation.compatibleWithSdk: ${this.compatibleWithSdk}")
            Log.d(TAG, "==== Dump automation.isValid: ${this.isValid}")
            Log.d(TAG, "==== Dump automation.validationIssues: ${this.validationIssues}")
            Log.d(TAG, "==== Dump automation.manuallyExecutable: ${this.manuallyExecutable}")
            Log.d(TAG, "==== Dump automation.isActive: ${this.isActive}")
            Log.d(TAG, "==== Dump automation.isRunning: ${this.isRunning}")
        }
        // Note: the first child container of an Automation is always an SequentialFlow
        val automationGraphSequentialFlow: SequentialFlow? = automation.automationGraph
        Log.d(TAG, "==== Dump automation.automationGraph: $automationGraphSequentialFlow")
        if (automationGraphSequentialFlow != null) {
            Log.d(TAG, "==== Dump automation.automationGraph.nodeId: ${automationGraphSequentialFlow.nodeId}")
            val automationNodes: List<Node> = automationGraphSequentialFlow.nodes
            automationNodes.forEach { node ->
                dumpAutomationNode(node)
            }
            Log.d(TAG, "==== Dump automation.automationGraph: Done")
        }
        Log.d(TAG, "=== Dump automation: Done")
    }
    private suspend fun dumpAutomationsInStructure(structure: Structure) {
        Log.i(TAG, "== Dump automations of structure: $structure")
        //
        if (true) { // dumpAutomation
            val automations: HomeObjectsFlow<Automation> = structure.automations()
            automations.list().forEach { automation ->
                dumpAutomation(automation)
            }
        }
        Log.i(TAG, "== Dump automations of the structure: Done")
    }
    // ============================================================================================
    // D&S API
    // ============================================================================================
    fun Set<HomeDevice>.diff(prevSet: Set<HomeDevice>): Unit {
        // Note: we can't compare device in deviceSet directly since they differ even if names are changed
        //     : Only device Ids keep persistent
        val removedIdSet = prevSet.map { it.id } - this.map { it.id }.toSet()
        val addedIdSet = this.map { it.id } - prevSet.map { it.id }.toSet()
        prevSet.forEach {
            if (it.id in removedIdSet) {
                Log.d(TAG, "testSimpleSubscribeStructureDeviceChanges: Got device removed! ${it.name}, ${it.id}")
            }
        }
        this.subtract(prevSet).forEach {
            if (it.id in addedIdSet) {
                Log.d(TAG, "testSimpleSubscribeStructureDeviceChanges: Got device newly added! ${it.name}, ${it.id}")
            } else {
                // Note: device changes will also be counted when deviceSet subtracting
                Log.d(TAG, "testSimpleSubscribeStructureDeviceChanges: Got device updated! ${it.name}, ${it.id}, structureId=${it.structureId}, roomId=${it.roomId}}")
            }
        }
    }

    private suspend fun testSimpleToggleOnOffLight(homeName: String?, deviceName: String?) {
        val structure = homeClient.structures().list().firstOrNull {
            // if name is null, choose first one
            homeName == null || it.name == homeName
        }.takeIf {
            it != null
        }
            ?: run {
                Log.w(TAG, "Not found Home name: $homeName")
                return
            }
        val lightDevice = structure.devices().list().firstOrNull {
            (
              // Must be one type of light
              it.has(OnOffLightDevice) ||
                it.has(DimmableLightDevice) ||
                it.has(ColorTemperatureLightDevice) ||
                it.has (ExtendedColorLightDevice)
              // it.has(Brightness)
              ) && (
              // if name is null, choose first one
              deviceName == null || it.name == deviceName
              )
        }.takeIf {
            it != null
        }
            ?: run {
                Log.w(TAG, "Not found Home device: $deviceName")
                return
            }
        // Toggle first light device OnOff
        lightDevice.traitFromType(OnOff, OnOffLightDevice).firstOrNull()?.toggle()
    }
    // TODO: Test this
    private suspend fun testSimpleDoorLock () {
        var doorLockDevice = homeClient.devices().list().first { device -> device.has(DoorLock) }
        val traitFlow: Flow<DoorLock?> =
            doorLockDevice.type(DoorLockDevice).map { it.standardTraits.doorLock }.distinctUntilChanged()
        val doorLockTrait: DoorLock = traitFlow.first()!!
        if (doorLockTrait.supports(DoorLock.Attribute.wrongCodeEntryLimit)) {
            val unused = doorLockTrait.update { setWrongCodeEntryLimit(3u) }
        }
    }
    // TODO: Test this
    private suspend fun testConstraint() {
        // Filter the output of candidates() to find the TraitAttributesCandidate
        // for the LevelControl trait.
        val structure = homeClient.structures().list().first()
        val levelCommand =
            structure
                .allCandidates()    // Flow<Set<NodeCandidate>>
                .first()            // Set<NodeCandidate>
                .firstOrNull { candidate ->   // NodeCandidate
                    candidate is CommandCandidate &&
                      candidate.commandDescriptor == LevelControlTrait.MoveToLevelCommand
                } as? CommandCandidate
        val levelConstraint : Constraint
// Get the NodeCandidate instance's fieldDetailsMap and
// retrieve the Constraint associated with the level parameter.
// In this case, it is a NumberRangeConstraint.
        if (levelCommand != null) {
            levelConstraint =
                levelCommand
                    .fieldDetailsMap[
                    LevelControlTrait
                        .MoveToLevelCommand
                        .Request
                        .CommandFields
                        .level
                ]!!
                    .constraint!! as NumberRangeConstraint<Int>
            val value = 0
            // ...
            // Test the value against the Constraint (ignoring step and unit)
            if (value in levelConstraint.lowerBound..levelConstraint.upperBound) {
                // TODO: How do I set attribute in action
                // ok to use the value
            }
        }
    }
    private suspend fun testSimpleTraitControl(deviceType: DeviceType, trait: Trait) {
        try {
            when (trait) {
                // Generic
                is Descriptor -> {
                }
                // com.google.home.google.*
                is ExtendedColorControl -> {
                }
                is LightEffects -> {
                }
                // com.google.home.matter.standard.*
                is BasicInformation -> {
                }
                is com.google.home.matter.standard.Descriptor -> {
                }
                is OnOff -> {
                    // Test toggle
                    trait.toggle()
                }
                is LevelControl -> {
                    trait.apply {
                        val optionsMask = LevelControlTrait.OptionsBitmap(
                            executeIfOff = trait.featureMap.onOff,
                            coupleColorTempToLevel = if (deviceType.trait(ColorControl) != null) true else false
                        )
                        val testMinLevel: UByte = 30u.toUByte()
                        val testMaxLevel: UByte = if (this.maxLevel != null) (this.maxLevel!! - 30u).toUByte()
                        else 100u.toUByte()
                        //
                        this.moveToLevel(
                            level = (
                              if (this.currentLevel != testMinLevel) {
                                  testMinLevel
                              } else
                                  testMaxLevel
                              ),
                            transitionTime = 100u,
                            optionsMask = optionsMask,
                            optionsOverride = optionsMask
                        )
                    }
                }
                is ColorControl -> {
                }
                else -> {
                    //
                }
            }
        } catch (e: HomeException) {
            Log.w(TAG, "====== testSimpleTraitControl: unable to control ${trait.factory}, error: $e")
        }
    }
    // TODO: to dump trait specific values by the trait type
    private suspend fun dumpTraitByType(deviceType: DeviceType, trait: Trait) {
        // TODO: Also show SimplifiedTraits
        // TODO: Dump FeatureMap
        try {
            when (trait) {
                // Generic
                is Descriptor -> {
                }
                // com.google.home.google.*
                is ExtendedColorControl -> {
                }
                is LightEffects -> {
                }
                // com.google.home.matter.standard.*
                is BasicInformation -> {
                }
                is com.google.home.matter.standard.Descriptor -> {
                }
                is OnOff -> {
                }
                is LevelControl -> {
                }
                is ColorControl -> {
                }
                else -> {
                    //
                }
            }
        } catch (e: HomeException) {
            Log.w(TAG, "====== dumpTraitByType: unable to show ${trait.factory}, error: $e")
        }
    }
    private suspend fun dumpTrait(deviceType: DeviceType, trait: Trait) {
        Log.d(TAG, "====== Dump Trait: $trait")
        Log.d(TAG, "====== Dump Trait metadata.sourceConnectivity: ${trait.metadata.sourceConnectivity}")
        // TODO: Subscribe Trait and show the updates
        if (true) {
            dumpTraitByType(deviceType, trait)
        }
    }
    private suspend fun dumpDeviceType(deviceType: DeviceType) {
        Log.i(TAG, "==== Dump DeviceType: $deviceType")
        deviceType.apply {
            Log.d(TAG, "====== Dump DeviceType factory: ${this.factory}")
            Log.d(TAG, "====== Dump DeviceType metadata: ${this.metadata}")
            Log.d(TAG, "====== Dump DeviceType metadata.isPrimaryType: ${this.metadata.isPrimaryType }")
            Log.d(TAG, "====== Dump DeviceType metadata.sourceConnectivity: ${this.metadata.sourceConnectivity}")
        }
        // TODO: Subscribe DeviceType and show the updates
        //
        val traitSet = deviceType.traits()
        traitSet.forEach { trait ->
            if (true) dumpTrait(deviceType, trait)
            if (false) testSimpleTraitControl(deviceType, trait)
        }
        Log.d(TAG, "==== Dump DeviceType: Done")
    }
    private suspend fun dumpDevice(device: HomeDevice) {
        Log.i(TAG, "=== Dump Device: $device")
        device.apply {
            Log.i(TAG, "==== Dump Device.id: ${this.id}")
            Log.i(TAG, "==== Dump Device.name: ${this.name}")
            Log.i(TAG, "==== Dump Device.isInStructure: ${this.isInStructure }")
            Log.i(TAG, "==== Dump Device.isMatterDevice: ${this.isMatterDevice}")
            Log.i(TAG, "==== Dump Device.isInRoom: ${this.isInRoom}")
            Log.i(TAG, "==== Dump Device.roomId: ${this.roomId}")
            Log.i(TAG, "==== Dump Device.sourceConnectivity ${device.sourceConnectivity}")
            Log.i(TAG, "==== Dump Device.getDeviceInfo: ${this.getDeviceInfo()}")
        }
        //
        val deviceTypesFlow = device.types()
        deviceTypesFlow.firstOrNull()?.forEach { deviceType ->
            //
            dumpDeviceType(deviceType)
        }
        device.types().collectLatest { devieTypes ->
            Log.d(TAG, "=== Dump Device: gotten latest devieTypes: ${devieTypes}")
        }
        Log.i(TAG, "=== Dump Device: Done")
    }
    private suspend fun dumpStructure(structure: Structure) {
        Log.i(TAG, "== Dump devices of structure: $structure")
        val devicesFlow = structure.devices()
        devicesFlow.list().forEach { device ->
            dumpDevice(device)
        }
        Log.i(TAG, "== Dump devices of structure: Done")
    }
    private suspend fun shortestPathFromHomeToAttribute() {
        Log.d(TAG, "shortestPathFromHomeToAttribute !!!! ")
        // Note: turn Flow of Set<Structure> to Flow of Structure
        /*
        homeClient.structures().map { it.firstOrNull() }.collect {
          println("Structure ${it.id} updated to ${it}")
        }
        */
        val trait: Trait? =
            homeClient.structures()   // HomeObjectsFlow<Structure>
                .firstOrNull()          // Set<Structure>
                ?.first()               // Structure
                ?.devices()             // HomeObjectsFlow<HomeDevice>
                ?.firstOrNull()         // Set<Device>
                ?.first()               // Device
                ?.types()               // Flow<Set<DeviceType>>
                ?.firstOrNull()         // Set<DeviceType>
                ?.first()               // DeviceType
                ?.traits()              // Set<Trait>
                ?.firstOrNull()         // Trait
        Log.d(TAG, "Trait is $trait")
        // e.g.: Trait is LevelControl(currentLevel=2, remainingTime=null, minLevel=null, maxLevel=null, currentFrequency=null, minFrequency=null, maxFrequency=null, options=null, onOffTransitionTime=null, onLevel=null, onTransitionTime=null, offTransitionTime=null, defaultMoveRate=null, startUpCurrentLevel=null, generatedCommandList=[], acceptedCommandList=[4, 5, 6], attributeList=[0, 65528, 65529, 65531, 65532, 65533], featureMap=Feature(onOff=false, lighting=false, frequency=false), clusterRevision=0)
        trait?.apply {
            Log.d(TAG, "Trait factory is $factory")
            // e.g. factory is LevelControl
            Log.d(TAG, "Trait metadata.sourceConnectivity is ${metadata.sourceConnectivity}")
            // e.g. metadata.sourceConnectivity is SourceConnectivity(connectivityState=OFFLINE, dataSourceLocality=REMOTE)
            Log.d(
                TAG,
                "Trait metadata.sourceConnectivity.connectivityState is ${metadata.sourceConnectivity?.connectivityState}"
            )
            // e.g. metadata.sourceConnectivity.connectivityState is OFFLINE
            /*
            ?.metadata
            ?.sourceConnectivity
            ?.dataSourceLocality
            ?.name.toString()
            */
        }
        Log.d(TAG, "shortestPathFromHomeToAttribute Done!!!! ")
    }
    fun dumpAll(structureName: String) {
        viewModelScope.launch {
            if (false) buildDeviceTree(homeClient.devices().list())
            //
            Log.i(TAG, "= Dump Structure And Automations ")
            //
            homeClient.structures().list().forEach { structure ->
                if (true) dumpAutomationsInStructure(structure)
                if (true) dumpStructure(structure)
            }
            Log.i(TAG, "= Dump Structure And Automations: Done)")
        }
    }
    fun testAll(structureName: String) {
        // Note: using viewModelScope will cause thread blocking in structure.createAutomation
        viewModelScope.launch {
            // Test D&S API
            if (false) shortestPathFromHomeToAttribute()
            if (false) testSimpleToggleOnOffLight(homeName = structureName, deviceName = null)
            // Test Automation API
            if (false) createTestAutomation("MyFirstAutomation")
            if (false) getAllAutomationForDevice()
            if (false) updateMyAutomation(id = null, name = "MyFirstAutomation")
            if (false) deleteMyAutomation(id = null, name = "MyFirstAutomation")
            if (true) deleteAllAutomation(structureName)
            val hour = 10
            val min = 50
            val automationName = "SimpleTimerAutomation$hour$min"
            if (false) createSimpleAutomationWithTimerAndLight(
                automationName,
                hour,
                min,
                structureName,
                homeName = structureName,
                deviceName = null
            )
            if (false) {
                val automationId: String = createSimpleAutomationLightOnMoveToLevel(
                    "createSimpleAutomationLightOnMoveToLevel",
                    structureName
                )
                Log.d(TAG, "createSimpleAutomationLightOnMoveToLevel : $automationId")
            }
            if (false) {
                val automationId: String = createSimpleAutomationOccupancySensorToggleLight(
                    "createSimpleAutomationOccupancySensorToggleLight",
                    structureName,
                    occupancySensorDeviceName = null,
                    lightDeviceName = null
                )
                Log.d(TAG, "createSimpleAutomationOccupancySensorToggleLight : $automationId")
            }
            if (false) {
                val automationId: String = createSimpleAutomationLightAndToggleLight(
                    "createSimpleAutomationLightAndToggleLight",
                    structureName,
                    lightDeviceName1 = "ExtendedColorLight",
                    lightDeviceName2 = "light2"
                )
                Log.d(TAG, "createSimpleAutomationLightAndToggleLight : $automationId")
            }
            if (false) {
                val automationId: String = createSimpleAutomationLightAndToggleLight2(
                    "createSimpleAutomationLightAndToggleLight2",
                    structureName,
                    lightDeviceName1 = "ExtendedColorLight",
                    lightDeviceName2 = "light2"
                )
                Log.d(TAG, "createSimpleAutomationLightAndToggleLight2 : $automationId")
            }
            if (false) {
                val automationId: String = createSimpleAutomationLightAndThermostatWithSimplifiedTrait(
                    "createSimpleAutomationLightAndThermostatWithSimplifiedTrait",
                    structureName,
                    lightDeviceName1 = "light2",
                    thermostatDeviceName = null
                )
                Log.d(TAG, "createSimpleAutomationLightAndThermostatWithSimplifiedTrait : $automationId")
            }
            if (true) {
                val automationId: String = createSimpleAutomationAllThermostatThermalSensorAndAllBlinds(
                    "createSimpleAutomationAllThermostatThermalSensorAndAllBlinds",
                    structureName,
                )
                Log.d(TAG, "createSimpleAutomationAllThermostatThermalSensorAndAllBlinds : $automationId")
            }
        }
    }
}

