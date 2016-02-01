/*
 * Copyright (c) 2013-2016 Commonwealth Computer Research, Inc. All rights reserved.
 */

package org.locationtech.geomesa.gs.wcs

import java.util.{HashMap => jHashMap, Map => jMap}

import org.apache.wicket.markup.html.form.validation.IFormValidator
import org.apache.wicket.markup.html.form.{Form, FormComponent}
import org.apache.wicket.model.PropertyModel
import org.geoserver.catalog.CoverageStoreInfo
import org.locationtech.geomesa.gs.GeoMesaStoreEditPanel
import org.locationtech.geomesa.raster.wcs.AccumuloUrl

class GeoMesaCoverageStoreEditPanel(componentId: String, storeEditForm: Form[_])
  extends GeoMesaStoreEditPanel(componentId, storeEditForm) {

  import org.locationtech.geomesa.accumulo.data.AccumuloDataStoreParams._

  val model = storeEditForm.getModel
  setDefaultModel(model)
  val storeInfo = storeEditForm.getModelObject.asInstanceOf[CoverageStoreInfo]
  storeInfo.getConnectionParameters.putAll(parseConnectionParametersFromURL(storeInfo.getURL))
  val paramsModel = new PropertyModel(model, "connectionParameters")
  val instanceId = addTextPanel(paramsModel, instanceIdParam)
  val zookeepers = addTextPanel(paramsModel, zookeepersParam)
  val user = addTextPanel(paramsModel, userParam)
  val password = addPasswordPanel(paramsModel, passwordParam)
  val auths = addTextPanel(paramsModel, authsParam)
  val visibilities = addTextPanel(paramsModel, visibilityParam)
  val tableName = addTextPanel(paramsModel, tableNameParam)
  val collectStats = addCheckBoxPanel(paramsModel, statsParam)

  val dependentFormComponents = Array[FormComponent[_]](instanceId, zookeepers, user, password, auths, visibilities, tableName)
  dependentFormComponents.map(_.setOutputMarkupId(true))

  storeEditForm.add(new IFormValidator() {
    def getDependentFormComponents = dependentFormComponents

    def validate(form: Form[_]) {
      val storeInfo = form.getModelObject.asInstanceOf[CoverageStoreInfo]
      val accumuloUrl = AccumuloUrl(user.getValue, password.getValue, instanceId.getValue,
        zookeepers.getValue, tableName.getValue, Some(auths.getValue).filterNot(_.isEmpty),
        Some(visibilities.getValue).filterNot(_.isEmpty), java.lang.Boolean.valueOf(collectStats.getValue))
      storeInfo.setURL(accumuloUrl.url)
    }
  })

  def parseConnectionParametersFromURL(url: String): jMap[String, String] = {
    val params = new jHashMap[String, String]
    if (url != null && url.startsWith("accumulo:")) {
      val parsed = AccumuloUrl(url)
      params.put("user", parsed.user)
      params.put("password", parsed.password)
      params.put("instanceId", parsed.instanceId)
      params.put("tableName", parsed.table)
      params.put("zookeepers", parsed.zookeepers)
      params.put("auths", parsed.auths.getOrElse(""))
      params.put("visibilities", parsed.visibilities.getOrElse(""))
      params.put("collectStats", parsed.collectStats.toString)
    }
    params
  }
}
