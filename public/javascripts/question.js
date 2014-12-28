$(document).ready(function (){

    $("div.hideable_box>div+div").fadeOut(0);

    // click the title
    // div.hideable_box>div
    // hide the box below it
    // div.hideable_box>div+div
    $(".hideable_box>.box_title").click(function (){
      $(this).siblings().fadeToggle(100);
     });

    $(".latex_edit_render_area").fadeOut(0);

    $("[id$='_edit'] .render_button").click(function (){
        var parentId = $(this).parent().parent().attr('id');
        $(this).text($(this).text() == "Edit" ? "Render" : "Edit");
        var textarea = $("#" + parentId + " textarea");
        var renderarea = $("#" + parentId + " .latex_edit_render_area");
        textarea.fadeToggle(10);
        renderarea.fadeToggle(10);
        var latex_code = textarea.val();
        renderarea.empty();
        renderarea.append(latex_code);
        MathJax.Hub.Queue(["Typeset",MathJax.Hub]); // refreshes the view in some sense
    });

    $("[id$='_edit'] .latex_edit_render_area").click(function() {
        var parentId = $(this).parent().attr('id');
        var textarea = $("#" + parentId + " textarea");
        var renderarea = $("#" + parentId + " .latex_edit_render_area");
        var button = $("#" + parentId + " .render_button");
        button.text(button.text() == "Edit" ? "Render" : "Edit");
        textarea.fadeToggle(10);
        renderarea.fadeToggle(10);
    });

    $("[id$='_edit'] .submit_button").click(function (){
        var parentId = $(this).parent().parent().attr('id');
        debugger;
        var key = parentId.match('.*(?=_edit)')[0].replace('_','.');
        var url = window.location.pathname;
        var textarea = $("#" + parentId + " textarea");
        var newValue = textarea.val();
        var data = {};
        data[key] = newValue;
        $.ajax({
          contentType: 'application/json',
          type: 'POST',
          url: url,
          data: JSON.stringify(data),
          success: function(d){
            location.reload();
          }
        });
    });



    //$("#latex_box #tags ul li:nth-child(1)").click(function() {   //this will apply to all anchor tags
    $("#latex_box #inputbuttons #button1").click(function() {   //this will apply to all anchor tags
       //$("#latex_box textarea").val($("#latex_box textarea").val()+'$x_1$');
       $("#latex_box textarea").insertAtCaret('_{n}',false);
    });

    //$("#latex_box #tags ul li:nth-child(2)").click(function() {   //this will apply to all anchor tags
    $("#latex_box #inputbuttons #button2").click(function() {   //this will apply to all anchor tags
       $("#latex_box textarea").insertAtCaret('\\int_{a}^{b} f(x)\\,dx',false);
    });

    $("#latex_box #inputbuttons #button3").click(function() {   //this will apply to all anchor tags
       $("#latex_box textarea").insertAtCaret('\\alpha',false);
    });

    $("#latex_box #inputbuttons #button4").click(function() {   //this will apply to all anchor tags
       $("#latex_box textarea").insertAtCaret('\\frac{d}{dx}',false);
    });

    $("#latex_box #inputbuttons #button5").click(function() {   //this will apply to all anchor tags
       $("#latex_box textarea").insertAtCaret('<font color="blue"> blue text </font>',true)
    });

    $("#latex_box #inputbuttons #button6").click(function() {   //this will apply to all anchor tags
       $("#latex_box textarea").insertAtCaret('\n\\begin{equation}\n f(x) = g(x) + a \n\\end{equation} \n',true)
    });

    $("#latex_box #inputbuttons #button7").click(function() {   //this will apply to all anchor tags
       $("#latex_box textarea").insertAtCaret('\\left(   \\right)',false)
    });

    $("#latex_box #inputbuttons #button8").click(function() {   //this will apply to all anchor tags
       $("#latex_box textarea").insertAtCaret('\\left[   \\right]',false)
    });

    $("#latex_box #inputbuttons #button9").click(function() {   //this will apply to all anchor tags
       $("#latex_box textarea").insertAtCaret('\\lim_{x \\rightarrow a}',false)
    });

    $("#latex_box #inputbuttons #button10").click(function() {   //this will apply to all anchor tags
       $("#latex_box textarea").insertAtCaret('\\infty',false)
    });

    $("#latex_box #inputbuttons #button11").click(function() {   //this will apply to all anchor tags
       $("#latex_box textarea").insertAtCaret('\\pi',false)
    });

    $("#latex_box #inputbuttons #button12").click(function() {   //this will apply to all anchor tags
       $("#latex_box textarea").insertAtCaret('\\beta',false)
    });

    $("#latex_box #inputbuttons #button13").click(function() {   //this will apply to all anchor tags
       $("#latex_box textarea").insertAtCaret('\\gamma',false)
    });

    $("#latex_box #inputbuttons #button14").click(function() {   //this will apply to all anchor tags
       $("#latex_box textarea").insertAtCaret('\\delta',false)
    });

    $("#latex_box #inputbuttons #button15").click(function() {   //this will apply to all anchor tags
       $("#latex_box textarea").insertAtCaret('\\epsilon',false)
    });

    $("#latex_box #inputbuttons #button16").click(function() {   //this will apply to all anchor tags
       $("#latex_box textarea").insertAtCaret('\n\\begin{align}\n f(x) &= g(x) + a \\\\ \n      &= \\cos(x) \n\\end{align} \n',true)
    });

    $("#latex_box #inputbuttons #button17").click(function() {   //this will apply to all anchor tags
       $("#latex_box textarea").insertAtCaret('^{p}',false)
    });

    $("#latex_box #inputbuttons #button18").click(function() {   //this will apply to all anchor tags
       $("#latex_box textarea").insertAtCaret('\\lambda',false)
    });

    $("#latex_box #inputbuttons #button19").click(function() {   //this will apply to all anchor tags
       $("#latex_box textarea").insertAtCaret('\\frac{d^{2}}{dx^{2}}',false);
    });

    $("#latex_box #inputbuttons #button20").click(function() {   //this will apply to all anchor tags
       $("#latex_box textarea").insertAtCaret('\\frac{a}{b}',false);
    });

    $("#latex_box #inputbuttons #button21").click(function() {   //this will apply to all anchor tags
       $("#latex_box textarea").insertAtCaret('\\mathbb{R}',false);
    });

    $("#latex_box #inputbuttons #button22").click(function() {   //this will apply to all anchor tags
       $("#latex_box textarea").insertAtCaret('\\mathbb{C}',false);
    });

    $("#latex_box #inputbuttons #button23").click(function() {   //this will apply to all anchor tags
       $("#latex_box textarea").insertAtCaret('\\leq',false);
    });

    $("#latex_box #inputbuttons #button24").click(function() {   //this will apply to all anchor tags
       $("#latex_box textarea").insertAtCaret('\\geq',false);
    });

    $("#latex_box #inputbuttons #button25").click(function() {   //this will apply to all anchor tags
       $("#latex_box textarea").insertAtCaret('>',false);
    });

    $("#latex_box #inputbuttons #button26").click(function() {   //this will apply to all anchor tags
       $("#latex_box textarea").insertAtCaret('<',false);
    });

    $("#latex_box #inputbuttons #button27").click(function() {   //this will apply to all anchor tags
       $("#latex_box textarea").insertAtCaret('\\sin(  )',false);
    });

    $("#latex_box #inputbuttons #button28").click(function() {   //this will apply to all anchor tags
       $("#latex_box textarea").insertAtCaret('\\cos(  )',false);
    });

    $("#latex_box #inputbuttons #button29").click(function() {   //this will apply to all anchor tags
       $("#latex_box textarea").insertAtCaret('\\sim',false);
    });

    $("#latex_box #inputbuttons #button30").click(function() {   //this will apply to all anchor tags
       $("#latex_box textarea").insertAtCaret('\\sum_{i = 0}^{N}',false);
    });

    $("#latex_box #inputbuttons #button31").click(function() {   //this will apply to all anchor tags
       $("#latex_box textarea").insertAtCaret('\\dots',false);
    });

    $("#latex_box #inputbuttons #button32").click(function() {   //this will apply to all anchor tags
       $("#latex_box textarea").insertAtCaret('<font color="red"> red text </font>',true)
    });

    $("#latex_box #inputbuttons #button33").click(function() {   //this will apply to all anchor tags
       $("#latex_box textarea").insertAtCaret('\\begin{bmatrix} a & b \\\\ c & d \\end{bmatrix}',false);
    });

    $("#latex_box #inputbuttons #button34").click(function() {   //this will apply to all anchor tags
       $("#latex_box textarea").insertAtCaret('f(x) = \\begin{cases} a &\\mbox{if } x \\geq 0 \\\\ b &\\mbox{if } x < 0 \\end{cases}',false);
    });

    $("#latex_box button").click(function() {   //this will apply to all anchor tags
       $("#latex_box p").empty();
       $("#latex_box p").append( $("#latex_box textarea").val() );
       MathJax.Hub.Queue(["Typeset",MathJax.Hub]); // refreshes the view in some sense
    });



    $("#courseSelect").on('change', function(e){
        var course = $("#courseSelect option:selected").text();
        $.ajax({
          type: 'GET',
          url: "/questions/search/term/" + course,
          //TODO: replace URL with play's javascriptRoutes
          success: function(d){
              debugger;
              d = d.sort();
              var term_selector = $("#termSelect")
              term_selector.empty()
              $.each(d, function(value, term) {
                var option = $('<option />').val(term).text(term);
                term_selector.append(option)
              })
              $("#termSelect option").change();
           }
        })
    })

    $("#termSelect").on('change', function(e){
        var course = $("#courseSelect option:selected").text();
        var term = $("#termSelect option:selected").text();
        $.ajax({
          type: 'GET',
          url: "/questions/search/year/" + course + "/" + term,
          //TODO: replace URL with play's javascriptRoutes
          success: function(d){
            debugger;
            var year_selector = $("#yearSelect")
            year_selector.empty()
            $.each(d, function(value, year) {
                var option = $('<option />').val(year).text(year);
                year_selector.append(option)
            })
            $("#yearSelect option").change();
          }
        })
    });

    $("#yearSelect").on('change', function(e){
        var course = $("#courseSelect option:selected").text();
        var term = $("#termSelect option:selected").text();
        var year = $("#yearSelect option:selected").text();
        $.ajax({
          type: 'GET',
          url: "/questions/search/question/" + course + "/" + term + "_" + year,
          //TODO: replace URL with play's javascriptRoutes
          success: function(d){
            d = d.sort();
            var question_selector = $("#questionSelect")
            question_selector.empty()
            $.each(d, function(value, question) {
                var option = $('<option />').val(question).text(question);
                question_selector.append(option)
            })
          }
        })
    });
})

$.fn.insertAtCaret = function (insertion,notMath) {
  return this.each(function(){
    if (document.selection) {
      //IE support
      this.focus();
      sel = document.selection.createRange();
      sel.text = insertion;
      this.focus();
    }else if (this.selectionStart || this.selectionStart == '0') {
      //MOZILLA/NETSCAPE support
      startPos = this.selectionStart;
      endPos = this.selectionEnd;
      scrollTop = this.scrollTop;
      if(notMath){
        this.value = this.value.substring(0, startPos) + insertion + this.value.substring(endPos,this.value.length);
        this.focus();
        this.selectionStart = startPos + insertion.length;
        this.selectionEnd = startPos + insertion.length;
      } 
      else {
      if (isInMathEnvironment(this.value.substring(0, startPos))) {
        this.value = this.value.substring(0, startPos) + insertion + ' ' + this.value.substring(endPos,this.value.length);
        this.focus();
        this.selectionStart = startPos + insertion.length + 1;
        this.selectionEnd = startPos + insertion.length + 1;
      }
      else {
        this.value = this.value.substring(0, startPos) + '$' + insertion + '$' + this.value.substring(endPos,this.value.length);
        this.focus();
        this.selectionStart = startPos + insertion.length + 2;
        this.selectionEnd = startPos + insertion.length + 2;
      }
    }
      this.scrollTop = scrollTop;
    } else {
      this.value += insertion;
      this.focus();
    }
  });
};

function isInMathEnvironment(aString) {
  //Check if the string contains an odd number of '$'s
  var n = aString.split('$').length - 1;
  var bool = (n%2 == 1);

  //Check if the cursor is in the {equation} env.
  var nb = aString.split('\\begin{equation}').length - 1;
  var ne = aString.split('\\end{equation}').length - 1;
  bool = bool | (nb - ne == 1);

  //Check if the cursor is in the {align} env.
  var nb = aString.split('\\begin{align}').length - 1;
  var ne = aString.split('\\end{align}').length - 1;
  bool = bool | (nb - ne == 1);

  return bool;

};
