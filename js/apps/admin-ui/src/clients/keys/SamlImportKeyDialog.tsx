import { AlertVariant } from "@patternfly/react-core";
import { FormProvider, useFormContext } from "react-hook-form";
import { useTranslation } from "react-i18next";

import { useAlerts } from "../../components/alert/Alerts";
import { ConfirmDialogModal } from "../../components/confirm-dialog/ConfirmDialog";
import { KeyForm } from "./GenerateKeyDialog";
import type { KeyTypes } from "./SamlKeys";
import { SamlKeysDialogForm, submitForm } from "./SamlKeysDialog";

type SamlImportKeyDialogProps = {
  id: string;
  attr: KeyTypes;
  onClose: () => void;
};

export const SamlImportKeyDialog = ({
  id,
  attr,
  onClose,
}: SamlImportKeyDialogProps) => {
  const { t } = useTranslation();
  const form = useFormContext<SamlKeysDialogForm>();
  const { handleSubmit } = form;

  const { addAlert, addError } = useAlerts();

  const submit = (form: SamlKeysDialogForm) => {
    submitForm(form, id, attr, (error) => {
      if (error) {
        addError("clients:importError", error);
      } else {
        addAlert(t("importSuccess"), AlertVariant.success);
      }
    });
  };

  return (
    <ConfirmDialogModal
      open={true}
      toggleDialog={onClose}
      continueButtonLabel="clients:import"
      titleKey="clients:importKey"
      onConfirm={() => {
        handleSubmit(submit)();
        onClose();
      }}
    >
      <FormProvider {...form}>
        <KeyForm useFile hasPem />
      </FormProvider>
    </ConfirmDialogModal>
  );
};
