package com.idega.block.text.presentation;


import java.sql.*;
import java.util.*;
import java.io.*;
import com.idega.util.*;
import com.idega.presentation.text.*;
import com.idega.presentation.*;
import com.idega.presentation.ui.*;
import com.idega.core.localisation.presentation.ICLocalePresentation;
import com.idega.core.localisation.business.ICLocaleBusiness;
import com.idega.block.text.data.*;
import com.idega.block.text.business.*;
import com.idega.core.user.data.User;
//import com.idega.jmodule.image.presentation.ImageInserter;
import com.idega.block.media.presentation.SimpleChooserWindow;
import com.idega.block.media.presentation.ImageInserter;
import com.idega.core.accesscontrol.business.AccessControl;
import com.idega.util.text.*;
import com.idega.idegaweb.presentation.IWAdminWindow;
import com.idega.idegaweb.IWResourceBundle;
import com.idega.idegaweb.IWBundle;
import com.idega.idegaweb.IWMainApplication;
import com.idega.core.data.ICFile;

/**
 * Title:
 * Description:
 * Copyright:    Copyright (c) 2000-2001 idega.is All Rights Reserved
 * Company:      idega
  *@author <a href="mailto:aron@idega.is">Aron Birkir</a>
 * @version 1.1
 */

public class TextEditorWindow extends IWAdminWindow{

  private final static String IW_BUNDLE_IDENTIFIER="com.idega.block.text";
  private boolean isAdmin = false;
  private boolean update = false;
  private boolean save = false;
  private int iUserId = -1;
  private int iObjInsId = -1;
  public  static String prmAttribute = "txe_attribute";
  public  static String prmTextId = "txep_txtextid";
  public  static String prmDelete = "txep_txdeleteid";
  public  static String prmLocale = "txep_localedrp";
  public  static String prmObjInstId = "txep_icobjinstid";
  private static String prmHeadline = "txep_headline";
  private static String prmBody = "txep_body";
  public static String imageAttributeKey = "txre_im_prop";
  //debug
  //private static String prmImageId = "txep.imageid";
  private static String prmImageId = "txep_imageid";
  private static String prmTxTextId = "txep_txtextid";
  private static String prmLocalizedTextId = "txep_loctextid";
  private static String prmUseImage = "txep_useimage";
  private static String actDelete = "txea_delete";
  private static String actSave = "txea_save";
  private static String actUpdate = "txea_update" ;
  private static String actNew = "txea_new";
  private static String modeNew = "txem_new";
  private static String modeDelete = "txem_delete";
  private TextHelper textHelper;

  private IWBundle iwb;
  private IWResourceBundle iwrb;

  public TextEditorWindow(){
    setWidth(570);
    setHeight(430);
    setUnMerged();
  }

  private void control(IWContext iwc)throws Exception{
    boolean doView = true;
    Locale currentLocale = iwc.getCurrentLocale(),chosenLocale;

    String sLocaleId = iwc.getParameter(prmLocale);
    String sAtt = null;

    // LocaleHandling
    int iLocaleId = -1;
    if(sLocaleId!= null){
      iLocaleId = Integer.parseInt(sLocaleId);
      chosenLocale = TextFinder.getLocale(iLocaleId);
    }
    else{
      chosenLocale = currentLocale;
      iLocaleId = ICLocaleBusiness.getLocaleId(chosenLocale);
    }

    if ( isAdmin ) {
      String sAction;
    // end of LocaleHandling

    // Text initialization
    String sTextId = null,sAttribute = null;
    String sLocTextId = iwc.getParameter(prmLocalizedTextId);
    sAttribute = iwc.getParameter(prmAttribute);

    // Text Id Request :
    if(iwc.getParameter(prmTxTextId) != null){
      sTextId = iwc.getParameter(prmTxTextId);
    }
    // Attribute Request :
    else if(iwc.getParameter(prmAttribute)!=null){

    }
    // Delete Request :
    else if(iwc.getParameter(prmDelete)!=null){
      sTextId = iwc.getParameter(prmDelete);
      //add(""+iObjInsId);
      confirmDelete(sTextId,iObjInsId);
      doView = false;
    }
    // Object Instance Request :
    else if(iwc.getParameter(prmObjInstId)!= null){
      iObjInsId = Integer.parseInt(iwc.getParameter(prmObjInstId ) );
    }

    // end of Text initialization

    // Form processing
    processForm(iwc,sTextId,sLocTextId, sAttribute);

    if(doView)
      doViewText(sTextId,sAttribute,chosenLocale,iLocaleId);
    }
    else {
      noAccess();
    }
  }

  // Form Processing :
  private void processForm(IWContext iwc,String sTextId,String sLocTextId,String sAttribute){

    // Save :
    if(iwc.getParameter(actSave)!=null || iwc.getParameter(actSave+".x")!=null ){
      saveText(iwc,sTextId,sLocTextId,sAttribute);
    }
    // Delete :
    else if(iwc.getParameter( actDelete )!=null || iwc.getParameter(actDelete+".x")!=null){
      try {
        if(iwc.getParameter(modeDelete)!=null){
          int I = Integer.parseInt(iwc.getParameter(modeDelete));
          deleteText(I);
        }
      }
      catch (Exception ex) {
        ex.printStackTrace();
      }
    }
    // New:
     /** @todo make possible */
    else if(iwc.getParameter( actNew ) != null || iwc.getParameter(actNew+".x")!= null){
      sTextId = null;sAttribute = null;
    }
    // end of Form Actions
  }

  private void doViewText(String sTextId,String sAttribute,Locale locale,int iLocaleId){
    ContentHelper contentHelper = null;
    TxText eTxText = null;
    int iTextId = -1;
    if(sTextId != null){
      iTextId = Integer.parseInt(sTextId);
      eTxText = TextFinder.getText(iTextId);
      if(iLocaleId > 0)
        contentHelper = TextFinder.getContentHelper(iTextId,iLocaleId);
      else
        contentHelper = TextFinder.getContentHelper(iTextId,locale);
    }
    else if(sAttribute != null){
      contentHelper = TextFinder.getContentHelper(sAttribute,iLocaleId);
    }

    addLocalizedTextFields(eTxText,contentHelper,iLocaleId,sAttribute,iObjInsId);
  }

  private void addLocalizedTextFields(TxText txText,ContentHelper contentHelper, int iLocaleId,String sAttribute,int iObjInsId){
    LocalizedText locText = null;
    boolean hasTxText = ( txText != null ) ? true: false;
    //boolean hasLocalizedText = ( locText != null ) ? true: false;
    boolean hasContent = ( contentHelper != null) ? true:false;
    if(hasContent)
      locText = contentHelper.getLocalizedText(TextFinder.getLocale(iLocaleId));
    boolean hasLocalizedText = ( locText !=null ) ? true:false;

		Link propslink = null;
    TextInput tiHeadline = new TextInput(prmHeadline);
    tiHeadline.setLength(40);
    tiHeadline.setMaxlength(255);
    List L = TextFinder.listOfLocales();
    DropdownMenu LocaleDrop = ICLocalePresentation.getLocaleDropdownIdKeyed(prmLocale);
    LocaleDrop.setToSubmit();
    LocaleDrop.setSelectedElement(Integer.toString(iLocaleId));

    TextArea taBody = new TextArea(prmBody,65,18);
    if ( hasLocalizedText ) {
      if ( locText.getHeadline() != null ) {
        tiHeadline.setContent(locText.getHeadline());
      }
      if ( locText.getBody() != null ) {
        taBody.setContent(locText.getBody());
      }
      addHiddenInput(new HiddenInput(prmLocalizedTextId,String.valueOf(locText.getID())));
    }

    if( hasTxText )
      addHiddenInput(new HiddenInput(prmTxTextId,Integer.toString(txText.getID())));
    if(sAttribute != null)
      addHiddenInput(new HiddenInput(prmAttribute,sAttribute));
    if(iObjInsId > 0)
      addHiddenInput(new HiddenInput(prmObjInstId,String.valueOf(iObjInsId)));

    SubmitButton save = new SubmitButton(iwrb.getImage("save.gif"),actSave);

    ImageInserter imageInsert = new ImageInserter();
    imageInsert.setImSessionImageName(prmImageId);
    imageInsert.setUseBoxParameterName(prmUseImage);
    imageInsert.setWindowClassToOpen(SimpleChooserWindow.class);
    imageInsert.setSelected(false);
    if ( hasContent ) {
      List files = contentHelper.getFiles();
      if(files != null){
        ICFile file1 = (ICFile) files.get(0);
        imageInsert.setImageId(file1.getID());
        Text properties = new Text("properties");
        propslink = com.idega.block.media.presentation.ImageAttributeSetter.getLink(properties,file1.getID(),imageAttributeKey);
      }
    }

    addLeft(iwrb.getLocalizedString("title","Title"),tiHeadline,true);
    addLeft(iwrb.getLocalizedString("locale","Locale"), LocaleDrop,true);
    addLeft(iwrb.getLocalizedString("body","Text"),taBody,true);
    addRight(iwrb.getLocalizedString("image","Image"),imageInsert,true);
		if(propslink != null)
      addRight("props",propslink,true);
    addSubmitButton(save);
  }

  private void noAccess() throws IOException,SQLException {
    addLeft(iwrb.getLocalizedString("no_access","Login first!"));
    this.addSubmitButton(new CloseButton(iwrb.getLocalizedString("close","Closee")));
  }


  private void confirmDelete(String sTextId,int iObjInsId ) throws IOException,SQLException {
    int iTextId = Integer.parseInt(sTextId);
    TxText  txText= TextFinder.getText(iTextId);

    if ( txText != null ) {
      addLeft(iwrb.getLocalizedString("text_to_delete","Text to delete"));
      addLeft(iwrb.getLocalizedString("confirm_delete","Are you sure?"));
      addSubmitButton(new SubmitButton(iwrb.getImage("delete.gif"),actDelete));
      addHiddenInput(new HiddenInput(modeDelete,String.valueOf(txText.getID())));
    }
    else {
      addLeft(iwrb.getLocalizedString("not_exists","Text already deleted or not available."));
      addSubmitButton(new CloseButton(iwrb.getImage("close.gif")));
    }
  }

  private void saveText(IWContext iwc,String sTxTextId,String sLocalizedTextId,String sAttribute){
    String sHeadline = iwc.getParameter( prmHeadline );
    String sBody = iwc.getParameter(prmBody );
    String sImageId = iwc.getParameter(prmImageId);
    String sLocaleId = iwc.getParameter(prmLocale);
    String sUseImage = iwc.getParameter(prmUseImage);

    if(sHeadline != null || sBody != null){
      int iTxTextId = sTxTextId!=null?Integer.parseInt(sTxTextId): -1;
      int iLocalizedTextId = sLocalizedTextId != null ? Integer.parseInt(sLocalizedTextId): -1;
      int iLocaleId = sLocaleId != null ? Integer.parseInt(sLocaleId):-1;
      int iImageId = sImageId != null ? Integer.parseInt(sImageId):-1;
      boolean bUseImage = sUseImage!= null?true:false;
      Vector files = null;
      try {
        ICFile file = new ICFile(iImageId);
        files = new Vector();
        files.add(file);
      }
      catch (SQLException ex) {

      }

      TextBusiness.saveText(iTxTextId,iLocalizedTextId,iLocaleId,iUserId,iObjInsId,null,null,sHeadline,"",sBody,sAttribute,files);

    }
    setParentToReload();
    close();
  }

  private void deleteText(int iTextId ) {
    TextBusiness.deleteText(iTextId);
    setParentToReload();
    close();
  }

  public void main(IWContext iwc) throws Exception {
    super.main(iwc);
    isAdmin = iwc.hasEditPermission(new TextReader());
    User u= com.idega.block.login.business.LoginBusiness.getUser(iwc);
    iUserId = u != null?u.getID():-1;
    isAdmin = true;
    iwb = getBundle(iwc);
    iwrb = getResourceBundle(iwc);
    addTitle(iwrb.getLocalizedString("text_editor","Text Editor"));
    control(iwc);
  }

  public String getBundleIdentifier(){
    return IW_BUNDLE_IDENTIFIER;
  }
}
